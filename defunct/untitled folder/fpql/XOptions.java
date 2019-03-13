package catdata.fpql;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.function.Function;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextField;

import catdata.Pair;
import catdata.Unit;
import catdata.ide.Language;
import catdata.ide.Options;

public class XOptions extends Options {

	private static final long serialVersionUID = 1L;

	@Override
	public String getName() {
		return Language.FPQL.toString();
	}
	
	public boolean fast_amalgams = true;
	public boolean validate_amalgams = false;
	public boolean reorder_joins = true;
	public final boolean x_text = true;
	public boolean x_graph = true;
	public boolean x_cat = false;
	public boolean x_tables = true;
	public boolean x_adom = false;
	public boolean x_typing = true;
	private boolean x_elements = true;
	public boolean x_json = true;
	public int MAX_PATH_LENGTH = 8;

	//@Override
	@Override
	public Pair<JComponent, Function<Unit, Unit>> display() {
		JSplitPane generalsplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		
		JPanel x_1 = new JPanel(new GridLayout(Options.biggestSize, 1));
		JPanel x_2 = new JPanel(new GridLayout(Options.biggestSize, 1));
		
		generalsplit.add(x_1);
		generalsplit.add(x_2);
	
		JCheckBox reorder_joins_box = new JCheckBox("", reorder_joins);
		JLabel reorder_joins_label = new JLabel("Re-order FROM clauses:");
		x_1.add(reorder_joins_label);
		x_2.add(reorder_joins_box);

		JCheckBox fast_amalgams_box = new JCheckBox("", fast_amalgams);
		JLabel fast_amalgams_label = new JLabel("Use fast amalgams on saturated presentations:");
		x_1.add(fast_amalgams_label);
		x_2.add(fast_amalgams_box);
		
		JCheckBox validate_amalgams_box = new JCheckBox("", validate_amalgams);
		JLabel validate_amalgams_label = new JLabel("Validate amalgams (if validating categories):");
		x_1.add(validate_amalgams_label);
		x_2.add(validate_amalgams_box);
	
		JCheckBox x_typing_box = new JCheckBox("", x_typing);
		JLabel typing_label = new JLabel("Type check:"); 
		x_1.add(typing_label);
		x_2.add(x_typing_box);
		

		JTextField x_path_length_box = new JTextField(Integer.toString(MAX_PATH_LENGTH), 12);
		JLabel x_path_length_label = new JLabel("Max path length:");
		x_2.add(wrap(x_path_length_box));
		x_1.add(x_path_length_label);
		
		JPanel xArea = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JCheckBox x_cat_box = new JCheckBox("Cat", x_cat);
		JCheckBox x_graph_box = new JCheckBox("Graph", x_graph);
		JCheckBox x_textual_box = new JCheckBox("Text", x_text);
		JCheckBox x_tabular_box = new JCheckBox("Table", x_tables);
		JCheckBox x_adom_box = new JCheckBox("ADom", x_adom);
		JCheckBox x_elem_box = new JCheckBox("Elements", x_elements);
		JCheckBox x_json_box = new JCheckBox("JSON", x_json);
		xArea.add(x_textual_box); xArea.add(x_graph_box); xArea.add(x_cat_box); xArea.add(x_tabular_box); xArea.add(x_adom_box); xArea.add(x_elem_box); xArea.add(x_json_box);	
		x_1.add(new JLabel("Viewers:"));
		x_2.add(xArea);
		
		for (int i = 0; i < Options.biggestSize - size(); i++) {
			x_1.add(new JLabel());
			x_2.add(new JLabel());
		}
			
		Function<Unit, Unit> fn = (Unit t) -> {
                    int mpl = MAX_PATH_LENGTH;
                    try {
                        try {
                            mpl = Integer.parseInt(x_path_length_box.getText());
                        } catch (NumberFormatException nfe) {
                        }
                        if (mpl < 1) {
                            mpl = MAX_PATH_LENGTH;
                        }
                    } catch (NumberFormatException nfe) {
                    }
                    MAX_PATH_LENGTH = mpl;
                    x_adom = x_adom_box.isSelected();
                    x_cat = x_cat_box.isSelected();
                    x_graph = x_graph_box.isSelected();
                    x_tables = x_tabular_box.isSelected();
                    x_adom = x_adom_box.isSelected();
                    fast_amalgams = fast_amalgams_box.isSelected();
                    validate_amalgams = validate_amalgams_box.isSelected();
                    reorder_joins = reorder_joins_box.isSelected();
                    x_typing = x_typing_box.isSelected();
                    x_elements = x_elem_box.isSelected();
                    x_json = x_json_box.isSelected();
                    
                    return new Unit();
                };

		return new Pair<>(generalsplit, fn);
	}


	@Override
	public int size() {
		return 6;
	} 

}
