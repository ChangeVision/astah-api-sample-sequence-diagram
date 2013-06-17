import java.awt.geom.Point2D;
import java.io.IOException;

import com.change_vision.jude.api.inf.editor.BasicModelEditor;
import com.change_vision.jude.api.inf.editor.ModelEditorFactory;
import com.change_vision.jude.api.inf.editor.SequenceDiagramEditor;
import com.change_vision.jude.api.inf.editor.TransactionManager;
import com.change_vision.jude.api.inf.exception.InvalidEditingException;
import com.change_vision.jude.api.inf.exception.InvalidUsingException;
import com.change_vision.jude.api.inf.exception.LicenseNotFoundException;
import com.change_vision.jude.api.inf.exception.ProjectLockedException;
import com.change_vision.jude.api.inf.exception.ProjectNotFoundException;
import com.change_vision.jude.api.inf.model.IClass;
import com.change_vision.jude.api.inf.model.ICombinedFragment;
import com.change_vision.jude.api.inf.model.ILifeline;
import com.change_vision.jude.api.inf.model.IMessage;
import com.change_vision.jude.api.inf.model.IModel;
import com.change_vision.jude.api.inf.model.INamedElement;
import com.change_vision.jude.api.inf.model.IOperation;
import com.change_vision.jude.api.inf.model.ISequenceDiagram;
import com.change_vision.jude.api.inf.presentation.ILinkPresentation;
import com.change_vision.jude.api.inf.presentation.INodePresentation;
import com.change_vision.jude.api.inf.presentation.IPresentation;
import com.change_vision.jude.api.inf.project.ProjectAccessor;
import com.change_vision.jude.api.inf.project.ProjectAccessorFactory;


public class SequenceSample {

	public static void main(String[] args) {
		SequenceSample sample = new SequenceSample();
		try {
			sample.create("SeqSample.asta");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (LicenseNotFoundException e) {
			e.printStackTrace();
		} catch (ProjectNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ProjectLockedException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * SeqSample.asta
	 * 		- Sequence Diagram1
	 * 		- Boundary0
	 * 		- Class1
	 * 			- add(param0:Boundary0):void
	 * 				- Sequence Diagram2
	 */
	public void create(String name) throws ClassNotFoundException, LicenseNotFoundException,
			ProjectNotFoundException, IOException, ProjectLockedException {
		
		ProjectAccessor projectAccessor = ProjectAccessorFactory.getProjectAccessor();
		try {
			projectAccessor.create(name);
			TransactionManager.beginTransaction();
			createModels();
			createSequenceDiagram();
			TransactionManager.endTransaction();
			projectAccessor.save();
			
			System.out.println("Create SeqSample.asta Project done.");
		} catch (InvalidUsingException e) {
			e.printStackTrace();
		} catch (InvalidEditingException e) {
			e.printStackTrace();
		} finally {
			TransactionManager.abortTransaction();
			projectAccessor.close();
		}
	}

	private void createModels() throws ProjectNotFoundException, ClassNotFoundException, InvalidEditingException {
		ProjectAccessor projectAccessor = ProjectAccessorFactory.getProjectAccessor();
		IModel project = projectAccessor.getProject();

		BasicModelEditor bme = ModelEditorFactory.getBasicModelEditor();
		IClass boundary = bme.createClass(project, "Boundary0");
		boundary.addStereotype("boundary");

		IClass cls1 = bme.createClass(project, "Class1");
		IOperation op = bme.createOperation(cls1, "add", "void");
		bme.createParameter(op, "param0", boundary);
	}

	private void createSequenceDiagram() throws ClassNotFoundException, ProjectNotFoundException,
			InvalidUsingException, InvalidEditingException {
		
		ProjectAccessor projectAccessor = ProjectAccessorFactory.getProjectAccessor();
		IModel project = projectAccessor.getProject();
		IClass cls1 = findNamedElement(project.getOwnedElements(), "Class1", IClass.class);
		IClass boundary = findNamedElement(project.getOwnedElements(), "Boundary0", IClass.class);
		IOperation op0 = findNamedElement(cls1.getOperations(), "add", IOperation.class);

		//create sequence diagram
		SequenceDiagramEditor de = projectAccessor.getDiagramEditorFactory().getSequenceDiagramEditor();
		ISequenceDiagram newDgm2 = de.createSequenceDiagram(op0, "Sequence Diagram2");
		newDgm2.getInteraction().setArgument("seq arg2");
		ISequenceDiagram newDgm = de.createSequenceDiagram(project, "Sequence Diagram1");

		//create lifelines
		INodePresentation objPs1 = de.createLifeline("", 0);
		INodePresentation objPs2 = de.createLifeline("object2", 150);
		INodePresentation objPs3 = de.createLifeline("", 300);
		INodePresentation objPs4 = de.createLifeline("object4", 450);
		INodePresentation objPs5 = de.createLifeline("object5", 600);
		ILifeline lifeline1 =  (ILifeline) objPs1.getModel();
		lifeline1.setBase(boundary);
		ILifeline lifeline3 = (ILifeline) objPs3.getModel();
		lifeline3.setBase(cls1);
		objPs5.setProperty("fill.color", "#00FF00");

		//create messages, combinedFragment, interactionUse, stateInvariant
		INodePresentation framePs = (INodePresentation) findPresentationByType(newDgm, "Frame");
		de.createMessage("beginMsg0", framePs, objPs1, 80);
		de.createCreateMessage("CreateMsg0", objPs1, objPs2, 100);
		ILinkPresentation msgPs = de.createMessage("", objPs2, objPs3, 160);
		msgPs.getSource().setProperty("fill.color", "#0000FF");
		IMessage msg = (IMessage) msgPs.getModel();
		msg.setAsynchronous(true);
		msg.setOperation(op0);
		msgPs.setProperty("parameter_visibility", "false");
		
		ILinkPresentation msgPs1 = de.createMessage("msg1", msgPs.getSource(), objPs4, 190);
		IMessage msg1 = (IMessage) msgPs1.getModel();
		msg1.setArgument("arg1");
		msg1.setGuard("guard1");
		msg1.setReturnValue("retVal1");
		msg1.setReturnValueVariable("retValVar1");
		de.createReturnMessage("retMsg1", msgPs1);
		ILinkPresentation msgPs11 = de.createMessage("msg11", msgPs1.getTarget(), objPs5, 190);
		de.createReturnMessage("retMsg11", msgPs11);
		
		INodePresentation combFragPs = de.createCombinedFragment("", "alt", new Point2D.Double(420, 250), 300, 200);
		ICombinedFragment combFrag = (ICombinedFragment) combFragPs.getModel();
		combFrag.getInteractionOperands()[0].setGuard("condition > 60");
		combFrag.addInteractionOperand("", "else");
		combFragPs.setProperty("operand.1.length", "100");
		
		de.createMessage("msg31", objPs4, objPs5, 270);
		ILinkPresentation msgPs32 = de.createMessage("msg32", objPs4, objPs5, 370);
		msgPs32.setProperty("font.color", "#FF0000");
		
		INodePresentation usePs = de.createInteractionUse("use1", "arg0", newDgm2, new Point2D.Double(10, 300), 250, 80);
		usePs.setProperty("fill.color", "#FF0000");	//red
		ILinkPresentation msgPs4 = de.createMessage("msg4", usePs, objPs3, 350);
		msgPs4.setProperty("line.color", "#FF0000");
		
		de.createStateInvariant(objPs5, "state1", 500);
		
		ILinkPresentation foundPs = de.createFoundMessage("foundMsg0", new Point2D.Double(10, 430), objPs2);
		IMessage foundMsg = (IMessage) foundPs.getModel();
		foundMsg.addStereotype("stereotype1");
		ILinkPresentation lostPs = de.createLostMessage("lostMsg0", objPs2, new Point2D.Double(300, 480));
		IMessage lostMsg = (IMessage) lostPs.getModel();
		BasicModelEditor bme = ModelEditorFactory.getBasicModelEditor();
		bme.createConstraint(lostMsg, "constraint1");
		de.createDestroyMessage("destroyMsg0", objPs1, objPs2, 550);
		de.createMessage("endMsg0", objPs5, framePs, 600);

		//create common elements
		INodePresentation notePs1 = de.createNote("note for lifeline", new Point2D.Double(700, 150));
		de.createNoteAnchor(notePs1, objPs5);
		INodePresentation notePs2 = de.createNote("note for message", new Point2D.Double(400, 600));
		notePs2.setProperty("fill.color", "#CC00CC");
		de.createNoteAnchor(notePs2, lostPs);
	}

	private <T extends INamedElement> T findNamedElement(INamedElement[] children, String name, Class<T> clazz) {
		for (INamedElement child : children) {
			if (clazz.isInstance(child) && child.getName().equals(name)) {
				return clazz.cast(child);
			}
		}
		return null;
	}

	private IPresentation findPresentationByType(ISequenceDiagram dgm, String type) throws InvalidUsingException {
		for (IPresentation ps : dgm.getPresentations()) {
			if (ps.getType().equals(type)) {
				return ps;
			}
		}
		return null;
	}
}
