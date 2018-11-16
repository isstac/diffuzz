/**
 * DataFilter class represents a filter that is applied to a data set
 * coming from the server side. It consists of a set of column and value
 * and virtually can handle any type of filtering. For example if you have
 * users and need filtering by name (e.g. Peter) and age (e.g. 20-30),
 * you will add 3 data filters:
 *   - name: Peter
 *   - minAge: 20
 *   - maxAge: 30 
 * On the server side you will modify the SQL query to apply filtering
 * by name and age.
 * 
 * (c) 2007 by Svetlin Nakov - http://www.nakov.com
 * National Academy for Software Development - http://academy.devbg.org 
 * This software is freeware. Use it at your own risk.
 */

package example.client;

import com.google.gwt.user.client.rpc.IsSerializable;

public class DataFilter implements IsSerializable {

	private String column;
	private String value;
	
	public DataFilter()
	{
	}

	public DataFilter(String column, String value) {
		this.column = column;
		this.value = value;
	}
	
	public String getColumn() {
		return column;
	}

	public void setColumn(String column) {
		this.column = column;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

}
