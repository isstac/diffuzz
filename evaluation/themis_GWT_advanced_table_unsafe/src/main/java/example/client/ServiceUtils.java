/**
 * Utility class for simplifying access to the singleton
 * instances of all asynchronous services in this GWT module.
 * 
 * (c) 2007 by Svetlin Nakov - http://www.nakov.com
 * National Academy for Software Development - http://academy.devbg.org 
 * This software is freeware. Use it at your own risk.
 */

package example.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.ServiceDefTarget;

public class ServiceUtils {
	
	private static TableModelServiceAsync tableModelServiceAsync;
	
	public static TableModelServiceAsync getTableModelServiceAsync(){
		if (tableModelServiceAsync == null) {
			tableModelServiceAsync = (TableModelServiceAsync) 
				GWT.create(TableModelService.class);
			ServiceDefTarget target = (
					ServiceDefTarget) tableModelServiceAsync;
			target.setServiceEntryPoint(GWT.getModuleBaseURL() + 
				"/UsersTableModelService");
		}
		return tableModelServiceAsync;
	}

}
