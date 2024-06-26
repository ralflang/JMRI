/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jmri.swing;

import java.awt.Component;
import javax.swing.CellEditor;
import javax.swing.JList;

/**
 *
 * @author Randall Wood
 */
public interface ListCellEditor<E> extends CellEditor {

    Component getListCellEditorComponent(JList<E> list, E value,
            boolean isSelected,
            int index);

    @Override
    E getCellEditorValue();
}
