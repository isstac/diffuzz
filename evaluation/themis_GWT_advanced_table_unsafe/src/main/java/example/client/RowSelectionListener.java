/**
 * Event listener interface for table row selection events.
 *
 * (c) 2007 by Svetlin Nakov - http://www.nakov.com
 * National Academy for Software Development - http://academy.devbg.org 
 * This software is freeware. Use it at your own risk.
 */

package example.client;

import java.util.EventListener;

public interface RowSelectionListener extends EventListener {

	/**
	 * Fired when the currently selected row in the table changes.
	 * 
	 * @param sender
	 *     the AdvancedTable widget sending the event
	 * @param row
	 *     the row identifier (primary key) of the row being selected
	 */
	void onRowSelected(AdvancedTable sender, String rowId);
}
