/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package com.archimatetool.viewpoint;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import com.archimatetool.editor.model.IModelImporter;
import com.archimatetool.model.IArchimatePackage;
import com.archimatetool.model.viewpoints.ViewpointManager;

/**
 * @author Guillermo Fuentes
 */
@SuppressWarnings("nls")
public class ViewpointImporter implements IModelImporter {

	String VIEWPOINT_EXTENSION_WILDCARD = "*.viewpoint"; //$NON-NLS-1$

	// ID -> Object lookup table
	Map<String, EObject> idLookup;

	@Override
	public void doImport() throws IOException {
		File file = askOpenFile();
		if (file == null) {
			return;
		}
		Document doc = null;
		try {
			doc = new SAXBuilder().build(file.toURI().toURL());
			Element rootElement = doc.getRootElement();
			ViewpointManager manager = ViewpointManager.INSTANCE;
			Map<String, EClass[]> conceptsMap = manager.getConceptsMap();
			for (Element xmlViewpoint : rootElement.getChildren("viewpoint")) { //$NON-NLS-1$

				String id = xmlViewpoint.getAttributeValue("id"); //$NON-NLS-1$
				if (id == null || "".equals(id)) { //$NON-NLS-1$
					System.err.println("Blank id for viewpoint"); //$NON-NLS-1$
					continue;
				}

				Element xmlName = xmlViewpoint.getChild("name"); //$NON-NLS-1$
				if (xmlName == null) {
					System.err.println("No name element for viewpoint"); //$NON-NLS-1$
					continue;
				}

				String name = xmlName.getText();
				if (name == null || "".equals(name)) { //$NON-NLS-1$
					System.err.println("Blank name for viewpoint"); //$NON-NLS-1$
					continue;
				}

				Viewpoint vp = new Viewpoint(id, name);

				for (Element xmlConcept : xmlViewpoint.getChildren("concept")) { //$NON-NLS-1$
					String conceptName = xmlConcept.getText();
					if (conceptName == null || "".equals(conceptName)) { //$NON-NLS-1$
						System.err.println("Blank concept name for viewpoint"); //$NON-NLS-1$
						continue;
					}

					if (conceptsMap.containsKey(conceptName)) {
						addCollection(vp, conceptName, conceptsMap);
					} else {
						EClass eClass = (EClass) IArchimatePackage.eINSTANCE.getEClassifier(conceptName);
						if (eClass != null) {
							addConcept(vp, eClass);
						} else {
							System.err.println("Couldn't get eClass: " + conceptName); //$NON-NLS-1$
						}
					}
				}
				manager.addViewpoint(vp);
			}
		} catch (JDOMException | IOException e) {
			return;
		}
	}

	private void addCollection(Viewpoint vp, String conceptName, Map<String, EClass[]> conceptsMap) {
		for (EClass eClass : conceptsMap.get(conceptName)) {
			addConcept(vp, eClass);
		}
	}

	private void addConcept(Viewpoint vp, EClass eClass) {
		vp.addEClass(eClass);
	}

	protected File askOpenFile() {
		FileDialog dialog = new FileDialog(Display.getCurrent().getActiveShell(), SWT.OPEN);
		dialog.setFilterExtensions(new String[] { VIEWPOINT_EXTENSION_WILDCARD, "*.*" }); //$NON-NLS-1$
		String path = dialog.open();
		return path != null ? new File(path) : null;
	}
}
