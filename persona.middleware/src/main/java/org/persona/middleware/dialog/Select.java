/*	
	Copyright 2008-2010 Fraunhofer IGD, http://www.igd.fraunhofer.de
	Fraunhofer-Gesellschaft - Institute of Computer Graphics Research 
	
	See the NOTICE file distributed with this work for additional 
	information regarding copyright ownership
	
	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at
	
	  http://www.apache.org/licenses/LICENSE-2.0
	
	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
*/
package org.persona.middleware.dialog;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.persona.middleware.PResource;
import org.persona.middleware.service.PropertyPath;
import org.persona.middleware.util.StringUtils;
import org.persona.ontology.expr.Restriction;

/**
 * An input control to be used if the user input is restricted to a fixed list of acceptable values. As it
 * allows multiple selections, it can be seen as an abbreviation for a {@link Repeat} control having only a
 * {@link Select1} as child.
 * 
 * @author mtazari
 */
public class Select extends Input {
	public static final String MY_URI = Form.PERSONA_DIALOG_NAMESPACE + "Select";

	/**
	 * The list of choices in a select control. List members must be instances of either {@link ChoiceItem}
	 * or {@link ChoiceList}.
	 */
	public static final String PROP_CHOICES = Form.PERSONA_DIALOG_NAMESPACE + "choices";
	
	/**
	 * Just for use by de-serializers.
	 */
	public Select() {
		super();
	}
	
	/**
	 * For use by applications.
	 * 
	 * @param parent The group to contain this select object.
	 * @param label The label.
	 * @param ref mandatory property path within the form data to which this select object refers.
	 * @param valueRestriction Optional local restrictions on the value of this select object.
	 * @param initialValue Optional initial / default value that will be made available in the form data.
	 */
	public Select(Group parent, Label label,
			PropertyPath ref, Restriction valueRestriction, Object initialValue) {
		super(MY_URI, parent, label, ref, valueRestriction, initialValue);
	}

	protected Select(String typeURI, Group parent, Label label,
			PropertyPath ref, Restriction valueRestriction, Object initialValue) {
		super(typeURI, parent, label, ref, valueRestriction, initialValue);
	}
	
	/**
	 * Adds the given choice item to the list of choices in this select control.
	 */
	public void addChoiceItem(ChoiceItem item) {
		if (item != null) {
			List l = (List) props.get(PROP_CHOICES);
			if (l == null) {
				l = new ArrayList();
				props.put(PROP_CHOICES, l);
			}
			l.add(item);
		}
	}
	
	/**
	 * Adds the given choice list as a sublist to the list of choices in this select control.
	 */
	public void addChoiceList(ChoiceList list) {
		if (list != null) {
			List l = (List) props.get(PROP_CHOICES);
			if (l == null) {
				l = new ArrayList();
				props.put(PROP_CHOICES, l);
			}
			l.add(list);
		}
	}
	
	/**
	 * Having the actual values that the user should select from among them, this method automatically
	 * generates the list of choices for this select by trying to derive a label for them. No sublists
	 * can be generated by this method.
	 * 
	 * @return true if the given parameter is a non empty array without any null element, false otherwise.
	 */
	public boolean generateChoices(Object[] elems) {
		if (elems == null  ||  elems.length == 0)
			return false;
		
		for (int i=0; i<elems.length; i++) {
			if (elems[i] == null)
				return false;
			String label = (elems[i] instanceof PResource)? ((PResource) elems[i]).getResourceLabel() : null;
			if (label == null)
				label = elems[i].toString();
			addChoiceItem(new ChoiceItem(
					StringUtils.deriveLabel(label),
					null, elems[i]));
		}
		
		return true;
	}
	
	/**
	 * If the restrictions defined or derivable for this select control can be determined and a certain
	 * list of allowed values can be derived from those restriction, those values will be passed to {@link
	 * #generateChoices(Object[])} to construct the list of choices.
	 */
	public boolean generateChoices() {
		Restriction r = getRestrictions();	
		return (r == null)? false : generateChoices(r.getEnumeratedValues());
	}
	
	/**
	 * Returns the list of choices in this select control. Each of the elements in the returned
	 * array is supposed to be an instance of either {@link ChoiceItem} or {@link ChoiceList}.
	 */
	public Label[] getChoices() {
		List l = (List) props.get(PROP_CHOICES);
		if (l == null)
			return new Label[0];
		
		return (Label[]) l.toArray(new Label[l.size()]);
	}
	
	/**
	 * Returns the maximum number of values that can be selected in the context of this select control.
	 * A negative integer is returned if there is no upper limit.
	 */
	public int getMaxCardinality() {
		Restriction r = getRestrictions();
		return (r == null)? -1 : r.getMaxCardinality();
	}

	/**
	 * Overrides {@link FormControl#getMaxLength()} by only considering the labels of the choices
	 * currently associated with this select control. 
	 */
	public int getMaxLength() {
		List l = (List) props.get(PROP_CHOICES);
		if (l == null)
			return -1;

		int res = -1;
		for (Iterator i=l.iterator();  i.hasNext(); ) {
			Object o = i.next();
			if (o instanceof Label) {
				int aux = ((Label) o).getMaxLength();
				if (aux > res)
					res = aux;
			}
		}
		return res;
	}
	
	/**
	 * Returns the minimum number of values that must be associated with this select control
	 * as selected values. A non-positive integer is returned if there is no lower limit.
	 */
	public int getMinCardinality() {
		Restriction r = getRestrictions();
		return (r == null)? -1 : r.getMinCardinality();
	}
	
	/**
	 * Checks if any sublist is contained in the list of choices in this select control.
	 */
	public boolean isMultilevel() {
		List l = (List) props.get(PROP_CHOICES);
		if (l == null)
			return false;
		
		for (Iterator i=l.iterator(); i.hasNext();)
			if (i.next() instanceof ChoiceList)
				return true;

		return false;
	}
	
	/**
	 * Just for use by de-serializers.
	 * @see org.persona.middleware.PResource#setProperty(String, Object)
	 */
	public void setProperty(String propURI, Object value) {
		if (PROP_CHOICES.equals(propURI)) {
			if (props.containsKey(propURI))
				return;
			else if (value instanceof List) {
				for (Iterator i=((List) value).iterator(); i.hasNext();) {
					Object o = i.next();
					if (!(o instanceof ChoiceItem)  &&  !(o instanceof ChoiceList))
						return;
				}
				props.put(propURI, value);
			} else if (value instanceof ChoiceItem  ||  value instanceof ChoiceList) {
				List l = new ArrayList(1);
				l.add(value);
				props.put(propURI, l);
			}
		} else
			super.setProperty(propURI, value);
	}
	
	/**
	 * Tries to find the hidden value associated with a choice item whose label has been given as input and then
	 * store that value as user input by calling {@link #storeUserInput(Object)}. If no hidden value was found, the
	 * label itself will be used as user input.
	 * 
	 * Note: for use by I/O handlers that can not handle the association between labels and values internally.
	 * 
	 * @return true, if the storage was successful, false otherwise.
	 */
	public boolean storeUserInputByLabelString(String selectedLabelString) {
		if (selectedLabelString != null) {
			List l = (List) props.get(PROP_CHOICES);
			if (l != null)
				for (Iterator i = l.iterator();  i.hasNext();) {
					Object label = i.next();
					if (label instanceof ChoiceList)
						label = ((ChoiceList) label).findItem(selectedLabelString);
					if (label instanceof ChoiceItem  &&  selectedLabelString.equals(label.toString()))
						return storeUserInput(((ChoiceItem) label).getValue());
				}
		}
		return storeUserInput(selectedLabelString);
	}
}
