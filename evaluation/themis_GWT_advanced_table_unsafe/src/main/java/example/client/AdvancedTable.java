/**
 * AdvancedTable is GWT table widget that supports data paging, filtering
 * and column sorting. Paging, filtering and sorting are done by the server
 * side. The table uses a data provider, the class TableModelService. 
 * 
 * How to use it:
 * 
 * AdvancedTable table = new AdvancedTable();
 * TableModelServiceAsync someTableService =
 *     <create table model service async>;
 * table.setTableModelService(usersTableService);
 * RootPanel.get().add(table);
 * 
 * (c) 2007 by Svetlin Nakov - http://www.nakov.com
 * National Academy for Software Development - http://academy.devbg.org 
 * This software is freeware. Use it at your own risk.
 */

package example.client;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SourcesTableEvents;
import com.google.gwt.user.client.ui.TableListener;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.HTMLTable.RowFormatter;

public class AdvancedTable extends Composite {
	
	private static final int DEFAULT_PAGE_SIZE = 6;
	private static final String DEFAULT_TABLE_WIDTH = "430px";
	private static final String DEFAULT_TABLE_HEIGHT = "210px";
	private static final int NAVIGATION_PANEL_HEIGHT = 26;
	private static final int STATUS_INFO = 1001;
	private static final int STATUS_ERROR = 1002;
	private static final int STATUS_WAIT = 1003;
	private static final String SORT_ASC_SYMBOL = " \u25b2";
	private static final String SORT_DESC_SYMBOL = " \u25bc";
	private static final String MARK_COLUMN_TITLE = "\u00bb";
	private static final int NO_ROW_SELECTED = -1;
	private static final String DEFAULT_ROW_STYLE = "advancedTableRow";
	private static final String SELECTED_ROW_STYLE = "advancedTableSelectedRow";
	private static final String NULL_DISPLAY_VALUE = "&nbsp;";

	private ScrollPanel scrollPanelGrid;
	private HorizontalPanel navigationPanel;
	private final Grid grid;
	private final Label statusLabel;
	private final Button buttonFirstPage;
	private final Button buttonPrevPage;
	private final Button buttonNextPage;
	private final Button buttonLastPage;
	
	private ArrayList rowSelectionListeners;
	
	private int pageSize = DEFAULT_PAGE_SIZE;
	private boolean firstColumnVisible = true;
	private boolean allowRowMark = false;
	private TableModelServiceAsync tableModelService;
	private TableColumn[] columns;
	private DataFilter[] filters;
	private String[][] pageRows;
	private int totalRowsCount;
	private int currentPageRowsCount;
	private int currentPageStartRow;
	private int currentPageIndex;
	private String sortColumnName;
	private boolean sortOrder;
	private int selectedRowIndex;
	private Set markedRows = new HashSet();
	
	public AdvancedTable() {
		super();
		
		final DockPanel contentDockPanel = new DockPanel();
		initWidget(contentDockPanel);
		contentDockPanel.setSize("100%", "100%");
		this.setSize(DEFAULT_TABLE_WIDTH, DEFAULT_TABLE_HEIGHT);
		
		scrollPanelGrid = new ScrollPanel();
		scrollPanelGrid.setSize("100%", "100%");
		contentDockPanel.add(scrollPanelGrid, DockPanel.CENTER);
		contentDockPanel.setCellWidth(scrollPanelGrid, "100%");
		contentDockPanel.setCellHeight(scrollPanelGrid, "100%");
		
		grid = new Grid();
		grid.setCellSpacing(0);
		grid.setBorderWidth(1);
		scrollPanelGrid.add(grid);
		grid.setSize("100%", "100%");
		
		// Display a preview of the table (when not in browser mode)
		if (! GWT.isScript()) {
			grid.resize(DEFAULT_PAGE_SIZE+1, 3);
			grid.setText(0, 0, "Column 1");
			grid.setText(0, 1, "Column 2");
			grid.setText(0, 2, "Column 3");
		}
		
		// Add event handler to perform sorting on header column click
		// and row selection on row click
		this.grid.addTableListener(new TableListener() {
			public void onCellClicked(SourcesTableEvents sender, 
					int row, int column) {
				AdvancedTable.this.cellClicked(row, column);
			}
		});

		navigationPanel = new HorizontalPanel();
		contentDockPanel.add(navigationPanel, DockPanel.SOUTH);
		navigationPanel.setSize("100%", "26px");
		contentDockPanel.setCellHeight(navigationPanel, "26px");
		contentDockPanel.setCellWidth(navigationPanel, "100%");
		contentDockPanel.setCellVerticalAlignment(navigationPanel,
			HasVerticalAlignment.ALIGN_BOTTOM);

		final Button buttonRefresh = new Button();
		navigationPanel.add(buttonRefresh);
		navigationPanel.setCellHeight(buttonRefresh, "23px");
		buttonRefresh.setSize("70", "23");
		navigationPanel.setCellVerticalAlignment(buttonRefresh, 
			HasVerticalAlignment.ALIGN_BOTTOM);
		buttonRefresh.addClickListener(new ClickListener() {
			public void onClick(Widget sender) {
				AdvancedTable.this.buttonRefreshClicked();
			}
		});
		buttonRefresh.setText("Refresh");
		
		statusLabel = new Label();
		navigationPanel.add(statusLabel);
		statusLabel.setHeight("20px");
		navigationPanel.setCellHeight(statusLabel, "23px");
		navigationPanel.setCellHorizontalAlignment(
			statusLabel, HasHorizontalAlignment.ALIGN_RIGHT);
		navigationPanel.setCellVerticalAlignment(
			statusLabel, HasVerticalAlignment.ALIGN_BOTTOM);
		showStatus("Table model service not available.", STATUS_ERROR);
		
		buttonFirstPage = new Button();
		navigationPanel.add(buttonFirstPage);
		navigationPanel.setCellHeight(buttonFirstPage, "23px");
		buttonFirstPage.setSize("25", "23");
		navigationPanel.setCellVerticalAlignment(buttonFirstPage, 
			HasVerticalAlignment.ALIGN_BOTTOM);
		buttonFirstPage.addClickListener(new ClickListener() {
			public void onClick(Widget sender) {
				AdvancedTable.this.buttonFirstPageClicked();
			}
		});
		navigationPanel.setCellHorizontalAlignment(
			buttonFirstPage, HasHorizontalAlignment.ALIGN_RIGHT);
		navigationPanel.setCellWidth(buttonFirstPage, "30px");
		buttonFirstPage.setText("<<");
		
		buttonPrevPage = new Button();
		navigationPanel.add(buttonPrevPage);
		navigationPanel.setCellHeight(buttonPrevPage, "23px");
		buttonPrevPage.setSize("20", "23");
		navigationPanel.setCellVerticalAlignment(buttonPrevPage, 
			HasVerticalAlignment.ALIGN_BOTTOM);
		buttonPrevPage.addClickListener(new ClickListener() {
			public void onClick(Widget sender) {
				AdvancedTable.this.buttonPrevPageClicked();
			}
		});
		navigationPanel.setCellHorizontalAlignment(
			buttonPrevPage, HasHorizontalAlignment.ALIGN_RIGHT);
		navigationPanel.setCellWidth(buttonPrevPage, "23px");
		buttonPrevPage.setText("<");
		
		buttonNextPage = new Button();
		navigationPanel.add(buttonNextPage);
		navigationPanel.setCellHeight(buttonNextPage, "23px");
		buttonNextPage.setSize("20", "23");
		navigationPanel.setCellVerticalAlignment(buttonNextPage, 
			HasVerticalAlignment.ALIGN_BOTTOM);
		buttonNextPage.addClickListener(new ClickListener() {
			public void onClick(Widget sender) {
				AdvancedTable.this.buttonNextPageClicked();
			}
		});
		navigationPanel.setCellHorizontalAlignment(
			buttonNextPage, HasHorizontalAlignment.ALIGN_RIGHT);
		navigationPanel.setCellWidth(buttonNextPage, "23px");
		buttonNextPage.setText(">");

		buttonLastPage = new Button();
		navigationPanel.add(buttonLastPage);
		navigationPanel.setCellHeight(buttonLastPage, "23px");
		buttonLastPage.setSize("25", "23");
		navigationPanel.setCellVerticalAlignment(buttonLastPage,
			HasVerticalAlignment.ALIGN_BOTTOM);
		buttonLastPage.addClickListener(new ClickListener() {
			public void onClick(Widget sender) {
				AdvancedTable.this.buttonLastPageClicked();
			}
		});
		navigationPanel.setCellHorizontalAlignment(
			buttonLastPage, HasHorizontalAlignment.ALIGN_RIGHT);
		navigationPanel.setCellWidth(buttonLastPage, "28px");
		buttonLastPage.setText(">>");
	}
	
	public int getPageSize() {
		return this.pageSize;
	}

	/**
	 * Allows modifying the default page size for this table.
	 */
	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
		// Display a preview of the table (when not in browser mode)
		if (! GWT.isScript()) {
			grid.resize(pageSize+1, 3);
		}		
	}
	
	/**
	 * @return the current table data source (TableModelService).
	 */
	public TableModelServiceAsync getTableModelService() {
		return this.tableModelService;
	}
	
	/**
	 * @return if the first table column is visible or hidden.
	 */
	public boolean isFirstColumnVisible() {
		return firstColumnVisible;
	}

	/**
	 * Shows or hides the first table column. The value in the first column
	 * is considered a row identifier (primary key). It can be visible or
	 * hidden from the user.
	 */
	public void setFirstColumnVisible(boolean firstColumnVisible) {
		if (this.tableModelService != null) {
			throw new IllegalStateException(
				"Can not modify the FirstColumnVisible property " +
				"after the TableModelService is assigned!");
		}
		this.firstColumnVisible = firstColumnVisible;
	}

	/**
	 * @return if the table allows row marking.
	 */
	public boolean isAllowRowMark() {
		return this.allowRowMark;
	}

	/**
	 * Shows or hides the "row mark" check box in front of each row.
	 */
	public void setAllowRowMark(boolean allowRowMark) {
		if (this.tableModelService != null) {
			throw new IllegalStateException(
				"Can not modify the AllowedRowMark property " +
				"after the TableModelService is assigned!");
		}
		this.allowRowMark = allowRowMark;
	}
	
	/**
	 * Sets a table model for this table, updates its columns and rows
	 * based on the information coming from the server and redraws the
	 * table contents (column titles and data rows).
	 */
	public void setTableModelService(TableModelServiceAsync tableModelService) {
		this.tableModelService = tableModelService;
		this.updateTableColumns(new AsyncCallback() {
			public void onFailure(Throwable caught) {
				AdvancedTable.this.showStatus(
					"Can not get table columns from the server.",
					STATUS_ERROR);
			}
			public void onSuccess(Object result) {
				AdvancedTable.this.updateTableData();
			}
		});
	}
	
	public void addRowSelectionListener(RowSelectionListener listener) {
	    if (this.rowSelectionListeners == null) {
	    	this.rowSelectionListeners = new ArrayList();
	    }
	    this.rowSelectionListeners.add(listener);
	}
	
	/**
	 * Updates and redraws the table columns based on the table data
	 * coming from the server.
	 */
	private void updateTableColumns(final AsyncCallback completedCallback) {
		this.tableModelService.getColumns(new AsyncCallback() {
			public void onFailure(Throwable caught) {
				completedCallback.onFailure(caught);
			}
			public void onSuccess(Object result) {
				TableColumn[] columns = (TableColumn[]) result;
				AdvancedTable.this.updateTableColumns(columns);
				completedCallback.onSuccess(result);
			}
		});
	}
	
	private void updateTableColumns(TableColumn[] newColumns) {
		// Copy all visible columns to this.columns (remove hidden
		// columns and show additional "mark" column if available)
		if (this.firstColumnVisible) {
			if (this.allowRowMark) {
				this.columns = new TableColumn[newColumns.length+1];
				this.columns[0] = new TableColumn("", MARK_COLUMN_TITLE);
				for (int i=0; i<newColumns.length; i++) {
					this.columns[i+1] = newColumns[i];
				}
			} else {
				this.columns = newColumns;
			}
		}
		else {
			if (this.allowRowMark) {
				this.columns = newColumns;
				this.columns[0] = new TableColumn("", MARK_COLUMN_TITLE);
			}
			else {
				this.columns = new TableColumn[newColumns.length-1];
				for (int i=0; i<this.columns.length; i++) {
					this.columns[i] = newColumns[i+1];
				}
			}
		}

		// No sorting is performed by default
		this.sortColumnName = null;
		
		// No row is selected by default
		this.selectedRowIndex = NO_ROW_SELECTED;
		
		// Clear the set or "marked" rows
		this.markedRows.clear();
		
		// Now display the new table columns are resize the table
		redrawTableColumns();
	}

	/**
	 * Updates and redraws the table data rows of the currently
	 * selected page based on the information from the table model
	 * coming from the server. This method should be called after
	 * each change of the data at the server side (e.g. when applying
	 * data filtering or need to refresh the table rows).
	 */
	public void updateTableData() {
		showStatus("Loading...", STATUS_WAIT);

		// Reset the active page number
		this.currentPageIndex = 0;
		
		// Initialize the number of rows in the table:
		// 1 header row + pageSize data rows
		grid.resizeRows(1 + this.pageSize);
		
		this.updateRowsCount(new AsyncCallback() {
			public void onFailure(Throwable caught) {
				AdvancedTable.this.showStatus(
					"Can not get table rows count from the server.",
					STATUS_ERROR);
			}
			public void onSuccess(Object result) {
				AdvancedTable.this.updateRows();
			}
		});
	}
	
	/**
	 * Applies a set of filters over the table data and redraws the table.
	 */
	public void applyFilters(DataFilter[] filters) {
		this.filters = filters;
		updateTableData();
	}
	
	public void applySorting(String sortColumnName, boolean sortOrder) {
		this.sortColumnName = sortColumnName;
		this.sortOrder = sortOrder;
		redrawColumnTitles();
		updateRows();
	}

	/**
	 * Retrieves the table columns from the server and redraws them.
	 */
	private void redrawTableColumns() {
		// Create the header row and adjust table columns number 
		if (grid.getRowCount() == 0) {
			grid.resizeRows(1);
		}
		grid.getRowFormatter().setStyleName(0, "advancedTableHeader");
		grid.resizeColumns(this.columns.length);
		
		// Fill the column titles
		redrawColumnTitles();
	}

	private void applySorting(String column) {
		if (column.equals(this.sortColumnName)) {
			applySorting(this.sortColumnName, ! this.sortOrder);
		} 
		else {
			applySorting(column, true);
		}
	}

	private void redrawColumnTitles() {
		for (int col=0; col<this.columns.length; col++) {
			TableColumn column = this.columns[col];
			String title = column.getTitle();
			if (column.getName().equals(this.sortColumnName)) {
				if (this.sortOrder) {
					title = title + SORT_ASC_SYMBOL;
				}
				else {
					title = title + SORT_DESC_SYMBOL;
				}
			}
			grid.setText(0, col, title);
		}
		grid.getCellFormatter().setHorizontalAlignment(
				0, 0, HasHorizontalAlignment.ALIGN_CENTER);
	}
	
	private void updateRowsCount(final AsyncCallback completedCallback) {
		this.tableModelService.getRowsCount(
				this.filters, new AsyncCallback() {
			public void onFailure(Throwable caught) {
				completedCallback.onFailure(caught);
			}
			public void onSuccess(Object result) {
				int count = ((Integer) result).intValue();
				AdvancedTable.this.totalRowsCount = count;
				completedCallback.onSuccess(result);
			}
		});
	}
	
	private void updateRows() {
		showStatus("Loading...", STATUS_WAIT);
		
		// Check for empty table - it is a special case
		if (this.totalRowsCount == 0) {
			currentPageRowsCount = 0;
			drawEmptyTable();
			this.selectedRowIndex = NO_ROW_SELECTED;
			redrawSelectedRow();
			return;
		}
		
		// Calculate current page index
		int pagesCount = calcPagesCount();
		if (this.currentPageIndex >= pagesCount) {
			this.currentPageIndex = pagesCount-1;
		}
		
		// Calculate start row and rows count for the server request 
		this.currentPageStartRow = this.currentPageIndex * this.pageSize;
		this.currentPageRowsCount = this.pageSize;
		if (this.currentPageStartRow+this.currentPageRowsCount > this.totalRowsCount) {
			this.currentPageRowsCount = this.totalRowsCount % this.pageSize;
		}
		
		// Asynchronously get rows from the server
		this.tableModelService.getRows(
				this.currentPageStartRow, this.currentPageRowsCount,
				this.filters, this.sortColumnName, this.sortOrder, 
				new AsyncCallback() {
			public void onFailure(Throwable caught) {
				AdvancedTable.this.showStatus(
					"Can not get table rows data from the server.",
					STATUS_ERROR);
			}
			public void onSuccess(Object result) {
				AdvancedTable.this.pageRows = (String[][]) result;
				AdvancedTable.this.redrawRows();
			}
		});
	}
	
	private int calcPagesCount() {
		int pagesCount = 
			(this.totalRowsCount + pageSize - 1) / this.pageSize;
		return pagesCount;
	}
	
	private void redrawNavigationArea() {
		int startRow = this.currentPageIndex * this.pageSize;
		String rowsInfo = "Rows " + (startRow+1) + "-" + 
			(startRow+this.currentPageRowsCount) + " of " +
			this.totalRowsCount;
		showStatus(rowsInfo, STATUS_INFO);
		
		int pagesCount = calcPagesCount();
		boolean enabledPrevFirstPage = 
			(pagesCount>0) && (this.currentPageIndex > 0);
		this.buttonFirstPage.setEnabled(enabledPrevFirstPage);
		this.buttonPrevPage.setEnabled(enabledPrevFirstPage);

		boolean enabledNextLastPage = 
			(pagesCount>0) && (this.currentPageIndex < pagesCount-1);
		this.buttonNextPage.setEnabled(enabledNextLastPage);
		this.buttonLastPage.setEnabled(enabledNextLastPage);		
	}

	private void redrawRows() {
		int startTableColumn = 0;
		if (this.allowRowMark) {
			startTableColumn = 1;
		}
		
		int startDataColumn = 0;
		if (! this.firstColumnVisible) {
			startDataColumn = 1;
		}
		
		for (int row=0; row<this.pageSize; row++) {
			if (row < this.currentPageRowsCount) {
				// Fill data row in the table
				for (int col=startTableColumn; col<this.columns.length; col++) {
					String cellValue = 
						this.pageRows[row][col - startTableColumn + startDataColumn];
					if (cellValue != null) {
						grid.setText(row+1, col, cellValue);
					} 
					else {
						grid.setHTML(row+1, col, NULL_DISPLAY_VALUE);
					}
				}
			} else {
				// Fill empty row in the table
				for (int col=0; col<this.columns.length; col++) {
					grid.setHTML(row+1, col, NULL_DISPLAY_VALUE);
				}
			}			
		}
		
		if (this.allowRowMark) {
			redrawCheckBoxes();
		}
		
		this.selectedRowIndex = NO_ROW_SELECTED;
		redrawSelectedRow();

		redrawNavigationArea();
		
		fixGridSize();
	}

	private void redrawCheckBoxes() {
		for (int row=0; row<this.pageSize; row++) {
			if (row < this.currentPageRowsCount) {
				final CheckBox checkBox = new CheckBox();
				String rowId = this.pageRows[row][0];
				if (this.markedRows.contains(rowId)) {
					checkBox.setChecked(true);
				}
				final int currentRow = row;
				checkBox.addClickListener(new ClickListener() {
					public void onClick(Widget sender) {
						checkBoxChanged(currentRow, checkBox.isChecked());
					}
				});
				grid.setWidget(row+1, 0, checkBox);
				grid.getCellFormatter().setHorizontalAlignment(
					row+1, 0, HasHorizontalAlignment.ALIGN_CENTER);
			}		
		}		
	}

	private void checkBoxChanged(int row, boolean checked) {
		String rowId = this.pageRows[row][0];
		if (checked) {
			this.markedRows.add(rowId);
		}
		else {
			this.markedRows.remove(rowId);
		}
	}




    /**
     * Override this to customize CSS styles on a row-by-row basis based on the underlying data for each row
     * @param rowData values in the cells (columns) of the row
     * @return the CSS style name to be applied to the row
     */
    protected String getRowStyle(String[] rowData) {
            return null;
    }

	/**
	 * Change the CSS styles of all rows, custom styles may be applied based on each row's 
	 * underlying data. The currently selected row gets a different CSS style. 
	 */
	private void redrawSelectedRow() {
		RowFormatter gridRowFormatter = grid.getRowFormatter();
		for (int row=1; row<=this.pageSize; row++) {
			if (row == this.selectedRowIndex) {
				gridRowFormatter.setStyleName(row, SELECTED_ROW_STYLE);
			}
			else {
                String customStyle = getRowStyle(pageRows[row-1]);
                gridRowFormatter.setStyleName(row, customStyle == null ?
DEFAULT_ROW_STYLE : customStyle);
			}
		}
	}

	private void drawEmptyTable() {
		for (int row=0; row<this.pageSize; row++) {
			for (int col=0; col<this.columns.length; col++) {
				grid.setHTML(row+1, col, NULL_DISPLAY_VALUE);
			}
		}
		redrawNavigationArea();
		showStatus("No data found.", STATUS_INFO);
		fixGridSize();
	}

	private void cellClicked(int row, int column) {
		if (row == 0) {
			if ((column == 0) && (this.allowRowMark)) {
				// Sorting by the "mark" column is not allowed
				return;
			}
			String columnName = this.columns[column].getName();
			this.applySorting(columnName);
		} else {
			if (row <= this.currentPageRowsCount) {
				selectRow(row);
			}
		}
		redrawSelectedRow();
	}

	private void selectRow(int rowIndex) {
		// Assign selected row index
		this.selectedRowIndex = rowIndex;
		
		// Fire onRowSelected() event for all selection listeners
		String rowId = getSelectedRowId(); 
		if (this.rowSelectionListeners != null) {
			for (int i=0; i<this.rowSelectionListeners.size(); i++) {
				RowSelectionListener listener = 
					(RowSelectionListener) this.rowSelectionListeners.get(i);
				listener.onRowSelected(this, rowId);
			}
		}
	}

	private void buttonFirstPageClicked() {
		this.currentPageIndex = 0;
		this.updateRows();		
	}

	private void buttonPrevPageClicked() {
		if (this.currentPageIndex > 0) {
			this.currentPageIndex--;
			this.updateRows();	
		}			
	}

	private void buttonNextPageClicked() {
		int pagesCount = calcPagesCount();
		if (this.currentPageIndex < pagesCount-1) {
			this.currentPageIndex++;
			this.updateRows();	
		}
	}

	private void buttonLastPageClicked() {
		int pagesCount = calcPagesCount();
		this.currentPageIndex = pagesCount;
		this.updateRows();
	}
	
	private void buttonRefreshClicked() {
		this.updateTableData();
	}
	
	private void showStatus(String text, int statusLevel) {
		if (statusLevel == STATUS_INFO) {
			this.statusLabel.setText(text);
		} 
		else if (statusLevel == STATUS_WAIT) {
			this.statusLabel.setText(text);
		} 
		else if (statusLevel == STATUS_ERROR) {
			this.statusLabel.setText("Error: " + text);
		} 
		else {
			throw new IllegalArgumentException("Illegal statusLevel.");
		}		
	}
	
	/**
	 * @return the row identifier (primary key) of the currently selected
	 * row. This is the value of the first column in the table. If no row
	 * is currently selected, the returned value is null.
	 */
	public String getSelectedRowId() {
		if (this.selectedRowIndex == NO_ROW_SELECTED) {
			return null;
		}
		else {
			String selectedRowId =
				this.pageRows[this.selectedRowIndex-1][0];
			return selectedRowId;
		}
	}
	
	/**
	 * @return a set of row identifiers that are currently marked in the table.
	 * Row identifiers are strings (primary key) that uniquely identifies a row.
	 */
	public Set getMarkedRows() {
		return this.markedRows;
	}
	
	/**
	 * Removes the "mark" from all the rows in the table.
	 */
	public void clearMarkedRows() {
		this.markedRows.clear();
		redrawCheckBoxes();
	}
	
	/**
	 * Marks all rows in the table matching the current filters without
	 * clearing the currently marked rows. Attention: This operation
	 * causes all data rows to be retrieved from the server side and
	 * this could be slow. 
	 */
	public void markAllRows() {
		// Asynchronously get all data rows from the server
		this.tableModelService.getRows(0, this.totalRowsCount, this.filters, null, false,
				new AsyncCallback() {
			public void onFailure(Throwable caught) {
				AdvancedTable.this.showStatus(
					"Can not get table data rows from the server.",
					STATUS_ERROR);
			}
			public void onSuccess(Object result) {
				String[][] allTableRows = (String[][]) result;
				AdvancedTable.this.markRows(allTableRows);
			}
		});
	}
	
	private void markRows(String[][] allTableRows) {
		for (int row=0; row<allTableRows.length; row++) {
			String rowId = allTableRows[row][0];
			this.markedRows.add(rowId);
		}
		redrawCheckBoxes();		
	}

	private void fixGridSize() {
		// Fix scroll panel width
		String originalWidth =
			DOM.getStyleAttribute(this.getElement(), "width");
		scrollPanelGrid.setWidth(originalWidth);

		// Fix scroll panel height
		String originalHeightStr =
			DOM.getStyleAttribute(this.getElement(), "height");
		if (originalHeightStr.endsWith("%")) {
			// Height is given in percentages --> use the height from the parent
			Element parentContainer = DOM.getParent(this.getElement());
			originalHeightStr = DOM.getElementProperty(parentContainer, "offsetHeight");
			// Fix the table height: convert percentages to pixels
			this.setHeight("" + originalHeightStr + "px");
		}
		else if (originalHeightStr.endsWith("px")) {
			// Height is given in pixels --> remove the "px" at the end
			originalHeightStr = originalHeightStr.substring(
				0, originalHeightStr.length()-2);
		}
		int originalHeight = Integer.parseInt(originalHeightStr);
		int newHeight = originalHeight - NAVIGATION_PANEL_HEIGHT;
		scrollPanelGrid.setHeight("" + newHeight + "px");
	}

	
}
