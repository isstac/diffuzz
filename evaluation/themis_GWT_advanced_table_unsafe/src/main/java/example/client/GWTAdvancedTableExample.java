/**
 * Example GWT module that shows how to use the GWT AdvancedTable widget
 * and its data provider - the TableModelService interface.
 * 
 * (c) 2007 by Svetlin Nakov - http://www.nakov.com
 * National Academy for Software Development - http://academy.devbg.org 
 * This software is freeware. Use it at your own risk.
 */

package example.client;

import java.util.Set;

import com.google.gwt.core.client.EntryPoint;
import example.client.AdvancedTable;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public class GWTAdvancedTableExample implements EntryPoint {
	
	private Label labelMessages;

	public void onModuleLoad() {
		RootPanel rootPanel = RootPanel.get();

		final AdvancedTable table = new AdvancedTable();
		TableModelServiceAsync usersTableService =
			ServiceUtils.getTableModelServiceAsync();
		table.setAllowRowMark(true);
		//table.setFirstColumnVisible(false);
		table.setTableModelService(usersTableService);
		table.addRowSelectionListener(new RowSelectionListener() {
			public void onRowSelected(AdvancedTable sender, String rowId) {
				labelMessages.setText("Row " + rowId + " selected.");
			}
		});
		
		rootPanel.add(table, 10, 65);
		table.setSize("402px", "175px");
		table.setPageSize(5);

		final HorizontalPanel horizontalPanel = new HorizontalPanel();
		rootPanel.add(horizontalPanel, 10, 34);
		horizontalPanel.setSize("402px", "23px");

		final Label labelFilter = new Label("Filter:");
		horizontalPanel.add(labelFilter);
		horizontalPanel.setCellVerticalAlignment(
			labelFilter, HasVerticalAlignment.ALIGN_MIDDLE);
		labelFilter.setWidth("50");

		final TextBox textBoxFilter = new TextBox();
		horizontalPanel.add(textBoxFilter);
		textBoxFilter.setWidth("100%");
		horizontalPanel.setCellWidth(textBoxFilter, "100%");

		final Button buttonApplyFilter = new Button();
		horizontalPanel.add(buttonApplyFilter);
		buttonApplyFilter.addClickListener(new ClickListener() {
			public void onClick(Widget sender) {
				String filterText = textBoxFilter.getText();
				DataFilter filter = new DataFilter("keyword", filterText); 
				DataFilter[] filters = {filter};
				table.applyFilters(filters);
				labelMessages.setText("Filter '" + filterText +"' applied.");
			}
		});
		buttonApplyFilter.setWidth("100");
		horizontalPanel.setCellWidth(buttonApplyFilter, "100");
		horizontalPanel.setCellHorizontalAlignment(
			buttonApplyFilter, HasHorizontalAlignment.ALIGN_RIGHT);
		buttonApplyFilter.setText("Apply Filter");

		final Button clearFilterButton = new Button();
		horizontalPanel.add(clearFilterButton);
		clearFilterButton.addClickListener(new ClickListener() {
			public void onClick(Widget sender) {
				table.applyFilters(null);
				textBoxFilter.setText("");
				labelMessages.setText("Filter cleaned.");
			}
		});
		clearFilterButton.setWidth("100");
		horizontalPanel.setCellWidth(clearFilterButton, "100");
		clearFilterButton.setText("Clear Filter");

		final Label labelTitle = new Label("Advanced GWT Table (Freeware)");
		rootPanel.add(labelTitle, 10, 10);

		final Label labelCopyright = new Label("(c) 2007 by Svetlin Nakov");
		rootPanel.add(labelCopyright, 236, 10);
		labelCopyright.setSize("176px", "19px");
		labelCopyright.setHorizontalAlignment(
			HasHorizontalAlignment.ALIGN_RIGHT);

		this.labelMessages = new Label("Event messages will appear here.");
		rootPanel.add(this.labelMessages, 10, 245);
		labelMessages.setSize("402px", "19px");

		final Button buttonMarkAll = new Button();
		rootPanel.add(buttonMarkAll, 10, 269);
		buttonMarkAll.addClickListener(new ClickListener() {
			public void onClick(Widget sender) {
				table.markAllRows();
			}
		});
		buttonMarkAll.setWidth("128px");
		buttonMarkAll.setText("Mark All");

		final Button buttonMarkNothing = new Button();
		rootPanel.add(buttonMarkNothing, 143, 269);
		buttonMarkNothing.addClickListener(new ClickListener() {
			public void onClick(Widget sender) {
				table.clearMarkedRows();
			}
		});
		buttonMarkNothing.setSize("136px", "24px");
		buttonMarkNothing.setText("Mark Nothing");

		final Button buttonShowMarked = new Button();
		rootPanel.add(buttonShowMarked, 284, 269);
		buttonShowMarked.addClickListener(new ClickListener() {
			public void onClick(Widget sender) {
				Set markedRows = table.getMarkedRows();
				Window.alert("Marked rows:" + markedRows.toString());				
			}
		});
		buttonShowMarked.setSize("128px", "24px");
		buttonShowMarked.setText("Show Marked");
	}

}
