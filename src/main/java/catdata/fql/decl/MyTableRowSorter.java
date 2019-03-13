package catdata.fql.decl;

import java.util.Comparator;

import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

/**
 * 
 * @author ryan
 *
 * Helper class for row sorting.
 */
class MyTableRowSorter extends TableRowSorter<TableModel> {

	public MyTableRowSorter(TableModel model) {
		super(model);
	}

	@Override
	protected boolean useToString(int c) {
		return false;
	}
	
	@Override 
	public Comparator<?> getComparator(int c) {
		return (Object o1, Object o2) -> {
                    try {
                        Integer i1 = Integer.parseInt(o1.toString());
                        Integer i2 = Integer.parseInt(o2.toString());
                        return i1.compareTo(i2);
                    } catch (Exception ex) {
                        try {
                            Double i1 = Double.parseDouble(o1.toString());
                            Double i2 = Double.parseDouble(o2.toString());
                            return i1.compareTo(i2);
                        } catch (Exception ex2) {
                            
//					if (o1 instanceof Integer && o2 instanceof Integer) {
//					return ((Integer)o1).compareTo((Integer)o2);
//			}
                        	return o1.toString().compareTo(o2.toString());
                        } }
                };
	}
}
