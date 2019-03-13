package catdata.fqlpp;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.function.Function;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextField;

import catdata.Pair;
import catdata.Unit;
import catdata.ide.Language;
import catdata.ide.Options;

public class FqlppOptions extends Options {

	private static final long serialVersionUID = 1L;

	@Override
	public String getName() {
		return Language.FQLPP.toString();
	}
	
	public final int MAX_NODES = 32;
	public final int MAX_EDGES = 128;
	public final int MAX_PATH_LENGTH = 8;
	public final int MAX_DENOTE_ITERATIONS = 65536;

	public final String useLineage = "Summary as ID";
	public final String piLineage = "Summary as ID";
	
	public final boolean VALIDATE = true;
	public final boolean set_textual = true;
	public final boolean fn_textual = true;
	public final boolean set_tabular = true;
	public final boolean fn_tabular = true;
	public final boolean cat_tabular = true;
	public final boolean cat_textual = true;
	public final boolean ftr_tabular = true;
	public final boolean ftr_textual = true;
	public final boolean cat_graph = true;
	public final boolean ftr_graph = true;
	public final boolean set_graph = true;
	public final boolean fn_graph = true;
	public final boolean trans_textual = true;
	public final boolean trans_tabular = true;
	public final boolean trans_graph = true;
	public final boolean cat_schema = true;
	public final boolean ftr_elements = true;
	public final boolean ftr_instance = true;
	public final boolean ftr_joined = true;
	public final boolean ftr_mapping = true;
	public final boolean trans_elements =  true;
	
	@Override
	public Pair<JComponent, Function<Unit, Unit>> display() {
		JPanel viewer1 = new JPanel(new GridLayout(Options.biggestSize, 1));
		JPanel viewer2 = new JPanel(new GridLayout(Options.biggestSize, 1));

		JSplitPane generalsplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		
		generalsplit.add(viewer1);
		generalsplit.add(viewer2);

		JComboBox<String> lineageBox = new JComboBox<>(new String[] {"Fresh IDs", "Lineage as ID", "Summary as ID"});
		lineageBox.setSelectedItem(useLineage);
		JLabel lineageLabel = new JLabel("Sigma ID creation strategy:");
		viewer1.add(lineageLabel);
		viewer2.add(lineageBox);
		
		JComboBox<String> pilineageBox = new JComboBox<>(new String[] {"Fresh IDs", "Lineage as ID", "Summary as ID"});
		pilineageBox.setSelectedItem(piLineage);
		JLabel pilineageLabel = new JLabel("Pi ID creation strategy:");
		viewer1.add(pilineageLabel);
		viewer2.add(pilineageBox);

		JCheckBox jcb = new JCheckBox("", VALIDATE);
		JLabel label5 = new JLabel("Validate categories/functors/etc:");
		viewer1.add(label5);
		viewer2.add(jcb); 

		JTextField plen = new JTextField(Integer.toString(MAX_PATH_LENGTH));
		JLabel label6 = new JLabel("Max path length:");
		viewer1.add(label6);
		viewer2.add(wrap(plen));

		JTextField iter = new JTextField(Integer.toString(MAX_DENOTE_ITERATIONS));
		JLabel label7 = new JLabel("Max iterations for left-kan:");
		viewer1.add(label7);
		viewer2.add(wrap(iter));		
		
		JPanel setArea = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JCheckBox set_textual_box = new JCheckBox("Text", set_textual);
		setArea.add(set_textual_box);
		JCheckBox set_tabular_box = new JCheckBox("Table", set_tabular);
		setArea.add(set_tabular_box);
		JCheckBox set_graph_box = new JCheckBox("Graph", set_graph);
		setArea.add(set_graph_box);

		JLabel set_label = new JLabel("Set viewer panels:");
		viewer1.add(set_label);
		viewer2.add(setArea);

		JPanel fnArea = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JCheckBox fn_textual_box = new JCheckBox("Text", fn_textual);
		fnArea.add(fn_textual_box);
		JCheckBox fn_tabular_box = new JCheckBox("Table", fn_tabular);
		fnArea.add(fn_tabular_box);
		JCheckBox fn_graph_box = new JCheckBox("Graph", fn_graph);
		fnArea.add(fn_graph_box);
		
		JLabel fn_label = new JLabel("Function viewer panels:");
		viewer1.add(fn_label);
		viewer2.add(fnArea);
		
				JPanel catArea = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JCheckBox cat_textual_box = new JCheckBox("Text", cat_textual);
		catArea.add(cat_textual_box);
		JCheckBox cat_tabular_box = new JCheckBox("Table", cat_tabular);
		catArea.add(cat_tabular_box);
		JCheckBox cat_graph_box = new JCheckBox("Graph", cat_graph);
		catArea.add(cat_graph_box);
		JCheckBox cat_schema_box = new JCheckBox("Schema", cat_schema);
		catArea.add(cat_schema_box);

		JLabel cat_label = new JLabel("Category viewer panels:");
		viewer1.add(cat_label);
		viewer2.add(catArea);

		JPanel ftrArea = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JCheckBox ftr_textual_box = new JCheckBox("Text", ftr_textual);
		ftrArea.add(ftr_textual_box);
		JCheckBox ftr_tabular_box = new JCheckBox("Table", ftr_tabular);
		ftrArea.add(ftr_tabular_box);
		JCheckBox ftr_graph_box = new JCheckBox("Graph", ftr_graph);
		ftrArea.add(ftr_graph_box);
		JCheckBox ftr_elements_box = new JCheckBox("Elements", ftr_elements);
		ftrArea.add(ftr_elements_box);
		JCheckBox ftr_instance_box = new JCheckBox("Instance", ftr_instance);
		ftrArea.add(ftr_instance_box);
		JCheckBox ftr_joined_box = new JCheckBox("Joined", ftr_joined);
		ftrArea.add(ftr_joined_box);
		JCheckBox ftr_mapping_box = new JCheckBox("Mapping", ftr_mapping);
		ftrArea.add(ftr_mapping_box);

		JLabel ftr_label = new JLabel("Functor viewer panels:");
		viewer1.add(ftr_label);
		viewer2.add(ftrArea);
		
		JPanel tArea = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JCheckBox t_textual_box = new JCheckBox("Text", trans_textual);
		tArea.add(t_textual_box);
		JCheckBox t_tabular_box = new JCheckBox("Table", trans_tabular);
		tArea.add(t_tabular_box);
		JCheckBox t_graph_box = new JCheckBox("Graph", trans_graph);
		tArea.add(t_graph_box);
		JCheckBox t_elements_box = new JCheckBox("Elements", trans_elements);
		tArea.add(t_elements_box);
		
		JLabel t_label = new JLabel("Function viewer panels:");
		t_label.setToolTipText("Sets which viewers to use for transforms.");
		viewer1.add(t_label);
		viewer2.add(tArea);

		for (int i = 0; i < Options.biggestSize - size(); i++) {
			viewer1.add(new JLabel());
			viewer2.add(new JLabel());
		}
		Function<Unit, Unit> fn = (Unit t) -> Unit.unit;

		return new Pair<>(generalsplit, fn);
	}


	@Override
	public int size() {
		return 10;
	} 

}
