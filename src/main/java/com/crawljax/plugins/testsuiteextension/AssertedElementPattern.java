package com.crawljax.plugins.testsuiteextension;

import java.util.ArrayList;

/**
 * AssertedElementPattern is used to store informations about a DOM element accessed pattern in a Selenium assertion.
 * This pattern contains: The asserted element node, its parent node, and its children nodes
 * 
 * @author Amin Milani Fard
 */
public class AssertedElementPattern {
	
	org.w3c.dom.Element sourceElement = null;
	// node info
	private String tagName = "";
	private String textContent = "";
	private ArrayList<String> attributes = new ArrayList<String>();
	// parent node info
	private String parentTagName = "";
	private String parentTextContent = "";
	private ArrayList<String> parentAttributes = new ArrayList<String>();
	// children nodes info
	private ArrayList<String> childrenTagName = new ArrayList<String>();
	private ArrayList<String> childrenTextContent = new ArrayList<String>();
	private ArrayList<ArrayList<String>> childrenAttributes = new ArrayList<ArrayList<String>>();

	public AssertedElementPattern(org.w3c.dom.Element sourceElement){
		this.sourceElement = sourceElement;
		// node info
		tagName = sourceElement.getTagName();
		textContent = sourceElement.getTextContent();
		for (int i=0; i<sourceElement.getAttributes().getLength();i++)
			attributes.add(sourceElement.getAttributes().item(i).toString());
		
		// parent node info
		parentTagName = sourceElement.getParentNode().getNodeName();
		parentTextContent = sourceElement.getParentNode().getTextContent();
		for (int i=0; i<sourceElement.getParentNode().getAttributes().getLength();i++)
			parentAttributes.add(sourceElement.getParentNode().getAttributes().item(i).toString());

		// children nodes info
		ArrayList<String> childAttributes = new ArrayList<String>();
		for (int i=0; i<sourceElement.getChildNodes().getLength();i++){
			childrenTagName.add(sourceElement.getChildNodes().item(i).getNodeName());
			childrenTextContent.add(sourceElement.getChildNodes().item(i).getTextContent());
			if (sourceElement.getChildNodes().item(i).getAttributes()!=null){
				for (int j=0; j<sourceElement.getChildNodes().item(i).getAttributes().getLength();j++)
					childAttributes.add(sourceElement.getChildNodes().item(i).getAttributes().item(j).toString());
				childrenAttributes.add(childAttributes);
			}
			childAttributes.clear();
		}
	}

	@Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AssertedElementPattern)) return false;

        AssertedElementPattern aep = (AssertedElementPattern) o;
        // considering only the structure (tag names of node, parent, and children) not the context
        if (!aep.tagName.equals(this.tagName) || !aep.parentTagName.equals(this.parentTagName) || !aep.childrenTagName.equals(this.childrenTagName)) {  
            return false;
        } 
        
        return true;
    }

    @Override
    public int hashCode() {
        return 1;
    }

	@Override
	public String toString() {
		return "AssertedElementPattern [tagName=" + tagName + ", textContent=" + textContent
				+ ", attributes=" + attributes + ", parentTagName="
				+ parentTagName + ", parentTextContent=" + parentTextContent
				+ ", parentAttributes=" + parentAttributes
				+ ", childrenTagName=" + childrenTagName
				+ ", childrenTextContent=" + childrenTextContent
				+ ", childrenAttributes=" + childrenAttributes + "]";
	}
	
}
