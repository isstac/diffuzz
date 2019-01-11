
/**
 * Sample reference implementation of the TableModelService class.
 * For simplicity it does not use database but shows how to implement
 * data paging, sorting and filtering.
 * 
 * (c) 2007 by Svetlin Nakov - http://www.nakov.com
 * National Academy for Software Development - http://academy.devbg.org 
 * This software is freeware. Use it at your own risk.
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import example.client.DataFilter;
import example.client.TableColumn;
import example.client.TableModelService;
import example.server.ReflectionUtils;
import example.server.User;
import example.server.UserComparator;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

public class UsersTableModelServiceImpl extends RemoteServiceServlet implements TableModelService {

    private static final long serialVersionUID = 1L;

    private TableColumn[] columns = new TableColumn[] { new TableColumn("Id", "#"),
            new TableColumn("FirstName", "First Name"), new TableColumn("LastName", "Last Name") };

//    private User[] allUsers = new User[] { new User(1, "Pesho", "Kirov"), new User(2, "Kiro", "Gogov"),
//            new User(3, "Ivan", "Petrov"), new User(4, "Ivan", "Ivanov"), new User(5, "Kiro", "Mentata"),
//            new User(6, "Mimi", "Kakina"), new User(7, "Kaka", "Mara"), new User(8, "Ded", "Moroz"),
//            new User(9, "Bay", "Ivan"), new User(10, "Pencho", "Penchev"), new User(11, "Mityu", "Mitkov"),
//            new User(12, "Bat", "Sali"), new User(13, "Kiro", null), new User(14, null, "Goshev"),
//            new User(15, null, "Kirov"),
//            new User(16, "Karabatama-Tafataga-Taratugarugatugata", "Kushata-Ratafata-Katamalata"),
//            new User(17, "Looooooooooooooooooooooooooooooooooooooooong",
//                    "Loooooooooooooooooooooooooooooooooooooooooooooooooong"), };
    
    private User[] allUsers;

    private List<User> filteredUsers;
    
    /*YN*/
    private void initRandomData(int numberOfUsers) {
        allUsers = new User[numberOfUsers];
        for (int i=0; i<numberOfUsers; i++) {
            allUsers[i] = new User(i, "Ivan", "Petrov");
        }
    }

    /*
     * YN: number of users is the secret and should be variable. It is only fixed in the implementation because it is a
     * dummy implementation. So for fuzzing we added back this variability.
     */
    public UsersTableModelServiceImpl(int numberOfUsers) {
        initRandomData(numberOfUsers);
        this.applyDataFilters(null);
    }

    public UsersTableModelServiceImpl() {
        this.applyDataFilters(null);
    }

    public TableColumn[] getColumns() {
        return this.columns;
    }

    public int getRowsCount(DataFilter[] filters) {
        applyDataFilters(filters);
        int count = this.filteredUsers.size();
        return count;
    }

    public String[][] getRows(int startRow, int rowsCount, DataFilter[] filters, String sortColumn, boolean sortOrder) {
        return getRows_unsafe(startRow, rowsCount, filters, sortColumn, sortOrder);
    }

    public String[][] getRows_unsafe(int startRow, int rowsCount, DataFilter[] filters, String sortColumn,
            boolean sortingOrder) {
        User[] rowsData = getRowsData(startRow, rowsCount, filters, sortColumn, sortingOrder);
        int columnsCount = this.columns.length;
        String[][] rows = new String[rowsCount][columnsCount];
        for (int row = 0; row < rowsCount; row++) {
            for (int col = 0; col < columnsCount; col++) {
                String columnName = this.columns[col].getName();
                rows[row][col] = ReflectionUtils.getPropertyStringValue(rowsData[row], columnName);
            }
        }
        return rows;
    }

    private User[] getRowsData(int startRow, int rowsCount, DataFilter[] filters, String sortColumn,
            boolean sortingOrder) {
        applyDataFilters(filters);
        applySorting(sortColumn, sortingOrder);
        User[] rows = new User[rowsCount];
        for (int row = startRow; row < startRow + rowsCount; row++) {
            rows[row - startRow] = this.filteredUsers.get(row);
        }
        return rows;
    }

    private void applyDataFilters(DataFilter[] filters) {
        this.filteredUsers = new ArrayList<User>();
        if (filters == null) {
            // No filter - append all users
            for (User user : this.allUsers) {
                this.filteredUsers.add(user);
            }
        } else {
            // Simulate data filtering
            String keyword = filters[0].getValue().toUpperCase();
            for (User user : this.allUsers) {
                String firstName = user.getFirstName();
                if (firstName == null) {
                    firstName = "";
                }
                String lastName = user.getLastName();
                if (lastName == null) {
                    lastName = "";
                }
                if (firstName.toUpperCase().contains(keyword) || lastName.toUpperCase().contains(keyword)) {
                    this.filteredUsers.add(user);
                }
            }
        }
    }

    private void applySorting(String sortColumn, boolean sortingOrder) {
        if (sortColumn != null) {
            UserComparator userComparator = new UserComparator(sortColumn, sortingOrder);
            Collections.sort(this.filteredUsers, userComparator);
        }
    }

}
