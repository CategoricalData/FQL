package catdata.opl;

import java.awt.GridLayout;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextField;

import catdata.Pair;
import catdata.Unit;
import catdata.Util;
import catdata.ide.Language;
import catdata.ide.Options;
import catdata.provers.KBOptions;

public class OplOptions extends Options implements Cloneable {

	private static final long serialVersionUID = 1L;	
	

	@Override 
	public Object clone() {
		try {
			Object ret = new OplOptions();
			for (Field f : getClass().getFields()) {
				f.set(ret, f.get(this));
			}
			return ret;
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		}
	}

	@Override
	public String getName() {
		return Language.OPL.toString();
	}

	public void set(String key, String value) {
		try {
			Field field = null;
			for (Field f : OplOptions.class.getFields()) {
				if (f.getName().equals(key)) {
					field = f;
					break;
				}
			}
			if (field == null) {
				throw new RuntimeException("No option named: " + key
						+ ".  Allowed: "
						+ Util.sep(Arrays.stream(OplOptions.class.getFields()).map(Field::getName).collect(Collectors.toList()), ", "));
			}
			if (field.getType().toString().equals("boolean")) {
				switch (value) {
					case "true":
						field.set(this, true);
						break;
					case "false":
						field.set(this, false);
						break;
					default:
						throw new RuntimeException(
								"Boolean-valued options must be true or false.");
				}
			} else if (field.getType().toString().equals("int")) {
				Integer i = Integer.parseInt(value);
				field.set(this, i);
			} else {
				throw new RuntimeException("Report this error to Ryan. " + field.getType());
			}
		} catch (IllegalAccessException | NumberFormatException ex) {
			ex.printStackTrace();
			throw new RuntimeException(ex.getMessage());
		}
	}

	public boolean opl_prover_compose = KBOptions.defaultOptions.compose;
	public boolean opl_prover_filter_subsumed = KBOptions.defaultOptions.filter_subsumed_by_self;
	public boolean opl_prover_unfailing = KBOptions.defaultOptions.unfailing;
	@SuppressWarnings("deprecation")
	public int opl_prover_timeout = KBOptions.defaultOptions.iterations;
	public boolean opl_prover_require_const = false;
	public boolean opl_prover_sort = KBOptions.defaultOptions.sort_cps;
	public boolean opl_prover_ac = KBOptions.defaultOptions.semantic_ac;
	@SuppressWarnings("deprecation")
	public int opl_prover_reduction_limit = KBOptions.defaultOptions.red_its;
	public int opl_saturate_timeout = 100000;
	public boolean opl_validate = true;
	public boolean opl_pretty_print = true;
	public boolean opl_reorder_joins = true;
	public boolean opl_suppress_dom = true;
	@SuppressWarnings("deprecation")
	public boolean opl_allow_horn = KBOptions.defaultOptions.horn;
	public boolean opl_query_check_eqs = true;
	public boolean opl_pushout_simpl = false;
	public boolean opl_lazy_gui = false;
	public boolean opl_cache_gui = false;
	public boolean opl_prover_force_prec = false;
	public boolean opl_require_consistency = false;
	public boolean opl_desugar_nat = true;
	public boolean opl_print_simplified_presentations = false;
	public boolean opl_display_fresh_ids = false;
	public boolean opl_prover_simplify_instances = false;
	public boolean opl_safe_java = true;
	public boolean opl_secret_agg = false;
	
	@Override
	public Pair<JComponent, Function<Unit, Unit>> display() {
		JPanel opl1 = new JPanel(new GridLayout(Options.biggestSize, 1));
		JPanel opl2 = new JPanel(new GridLayout(Options.biggestSize, 1));

		JSplitPane oplsplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

		oplsplit.add(opl1);
		oplsplit.add(opl2);

		JCheckBox opl_validate_box = new JCheckBox("", opl_validate);
		JLabel opl_validate_label = new JLabel("Validate mappings:");
		opl1.add(opl_validate_label);
		opl2.add(opl_validate_box);

		JCheckBox opl_unfailing_box = new JCheckBox("", opl_prover_unfailing);
		JLabel opl_unfailing_label = new JLabel("Allow unorientable equations:");
		opl1.add(opl_unfailing_label);
		opl2.add(opl_unfailing_box);

		JCheckBox opl_const_box = new JCheckBox("", opl_prover_require_const);
		JLabel opl_const_label = new JLabel(
				"Require a constant at each sort (false=dangerous):");
		opl2.add(opl_const_box);
		opl1.add(opl_const_label);

		JCheckBox opl_reorder_box = new JCheckBox("", opl_reorder_joins);
		JLabel opl_reorder_label = new JLabel("Reorder joins:");
		opl2.add(opl_reorder_box);
		opl1.add(opl_reorder_label);

		JCheckBox opl_semantic_ac_box = new JCheckBox("", opl_prover_ac);
		JLabel opl_semantic_ac_label = new JLabel(
				"Enable Semantic AC optimization in Knuth-Bendix:");
		opl2.add(opl_semantic_ac_box);
		opl1.add(opl_semantic_ac_label);

		JTextField opl_iterations_box = new JTextField(
				Integer.toString(opl_prover_timeout), 12);
		JLabel opl_iterations_label = new JLabel("Knuth-Bendix timeout (ms)");
		opl2.add(wrap(opl_iterations_box));
		opl1.add(opl_iterations_label);

		JTextField opl_homit_box = new JTextField(
				Integer.toString(opl_saturate_timeout), 12);
		JLabel opl_homit_label = new JLabel("Saturation timeout (ms)");
		opl2.add(wrap(opl_homit_box));
		opl1.add(opl_homit_label);

		JTextField opl_red_box = new JTextField(
				Integer.toString(opl_prover_reduction_limit), 12);
		JLabel opl_red_label = new JLabel("Reduction iterations maximum");
		opl2.add(wrap(opl_red_box));
		opl1.add(opl_red_label);

		JCheckBox opl_pretty_box = new JCheckBox("", opl_pretty_print);
		JLabel opl_pretty_label = new JLabel("Pretty Print terms:");
		opl2.add(opl_pretty_box);
		opl1.add(opl_pretty_label);

		JCheckBox opl_suppress_box = new JCheckBox("", opl_suppress_dom);
		JLabel opl_suppress_label = new JLabel("Supress instance domains:");
		opl2.add(opl_suppress_box);
		opl1.add(opl_suppress_label);

		JCheckBox opl_horn_box = new JCheckBox("", opl_allow_horn);
		JLabel opl_horn_label = new JLabel(
				"Allow implications in theories (dangerous, also can't check mappings):");
		opl2.add(opl_horn_box);
		opl1.add(opl_horn_label);

		JCheckBox opl_eqs_box = new JCheckBox("", opl_query_check_eqs);
		JLabel opl_eqs_label = new JLabel(
				"Check that queries preserve equalities:");
		opl2.add(opl_eqs_box);
		opl1.add(opl_eqs_label);

		JCheckBox opl_simpl_box = new JCheckBox("", opl_pushout_simpl);
		JLabel opl_simpl_label = new JLabel("Simplify pushout schemas:");
		opl2.add(opl_simpl_box);
		opl1.add(opl_simpl_label);

		JCheckBox opl_lazy_box = new JCheckBox("", opl_lazy_gui);
		JLabel opl_lazy_label = new JLabel("Lazily compute gui:");
		opl2.add(opl_lazy_box);
		opl1.add(opl_lazy_label);

		JCheckBox opl_sort_box = new JCheckBox("", opl_prover_sort);
		JLabel opl_sort_label = new JLabel(
				"In prover, sort critical pairs by length:");
		opl2.add(opl_sort_box);
		opl1.add(opl_sort_label);

		JCheckBox opl_selfsub_box = new JCheckBox("",
				opl_prover_filter_subsumed);
		JLabel opl_selfsub_label = new JLabel(
				"In prover, filter self-subsumed equations:");
		opl2.add(opl_selfsub_box);
		opl1.add(opl_selfsub_label);

		JCheckBox opl_compose_box = new JCheckBox("", opl_prover_compose);
		JLabel opl_compose_label = new JLabel("In prover, compose equations:");
		opl2.add(opl_compose_box);
		opl1.add(opl_compose_label);

		JCheckBox opl_prec_box = new JCheckBox("", opl_prover_force_prec);
		JLabel opl_prec_label = new JLabel(
				"In prover, force gens < consts < atts < fkeys precedence:");
		opl2.add(opl_prec_box);
		opl1.add(opl_prec_label);

		JCheckBox opl_cache_box = new JCheckBox("", opl_cache_gui);
		JLabel opl_cache_label = new JLabel("Cache artifacts between runs (true=not tested thoroughly):");
		opl2.add(opl_cache_box);
		opl1.add(opl_cache_label);
		
		JCheckBox opl_consistency_box = new JCheckBox("", opl_require_consistency);
		JLabel opl_consistency_label = new JLabel("Ensure consistency by requiring free type algebras:");
		opl2.add(opl_consistency_box);
		opl1.add(opl_consistency_label);

		
		JCheckBox opl_nat_sugar_box = new JCheckBox("", opl_desugar_nat);
		JLabel opl_nat_sugar_label = new JLabel("When possible desugar numerals into zero:Nat and succ:Nat->Nat:");
		opl2.add(opl_nat_sugar_box);
		opl1.add(opl_nat_sugar_label);

		JCheckBox opl_print_simpl_pres_box = new JCheckBox("", opl_print_simplified_presentations);
		JLabel opl_print_simpl_pres_label  = new JLabel("Print simplified presentations (true=not canonical):");
		opl2.add(opl_print_simpl_pres_box);
		opl1.add(opl_print_simpl_pres_label);
		
		JCheckBox opl_fresh_ids_box = new JCheckBox("", opl_display_fresh_ids);
		JLabel opl_fresh_ids_label  = new JLabel("Display fresh IDs:");
		opl2.add(opl_fresh_ids_box);
		opl1.add(opl_fresh_ids_label);
		
		JCheckBox opl_simplify_box = new JCheckBox("", opl_prover_simplify_instances);
		JLabel opl_simplify_label  = new JLabel("Simplify instances before doing theorem proving:");
		opl2.add(opl_simplify_box);
		opl1.add(opl_simplify_label);
		
		JCheckBox opl_safejava_box = new JCheckBox("", opl_safe_java);
		JLabel opl_safejava_label  = new JLabel("Require use of java typesides to be complete (false=dangerous):");
		opl2.add(opl_safejava_box);
		opl1.add(opl_safejava_label);	
		
		JCheckBox opl_agg_box = new JCheckBox("", opl_secret_agg);
		JLabel opl_agg_label  = new JLabel("Allow ad-hoc aggregation (true=dangerous):");
		opl2.add(opl_agg_box);
		opl1.add(opl_agg_label);

		for (int i = 0; i < Options.biggestSize - size(); i++) {
			opl1.add(new JLabel());
			opl2.add(new JLabel());
		}

		Function<Unit, Unit> fn = (Unit t) -> {
                    try {
                        int opl = opl_prover_timeout;
                        int opl_h = opl_saturate_timeout;
                        int opl_r = opl_prover_reduction_limit;
                        try {
                            opl = Integer.parseInt(opl_iterations_box.getText());
                            opl_h = Integer.parseInt(opl_homit_box.getText());
                            opl_r = Integer.parseInt(opl_red_box.getText());
                        } catch (NumberFormatException nfe) {
                        }
                        opl_prover_timeout = opl;
                        opl_saturate_timeout = opl_h;
                        opl_prover_reduction_limit = opl_r;
                    } catch (NumberFormatException nfe) {
                    }
                    
                    opl_prover_require_const = opl_const_box.isSelected();
                    opl_prover_sort = opl_sort_box.isSelected();
                    opl_prover_unfailing = opl_unfailing_box.isSelected();
                    opl_validate = opl_validate_box.isSelected();
                    opl_pretty_print = opl_pretty_box.isSelected();
                    opl_reorder_joins = opl_reorder_box.isSelected();
                    opl_suppress_dom = opl_suppress_box.isSelected();
                    opl_allow_horn = opl_horn_box.isSelected();
                    opl_prover_ac = opl_semantic_ac_box.isSelected();
                    opl_query_check_eqs = opl_eqs_box.isSelected();
                    opl_pushout_simpl = opl_simpl_box.isSelected();
                    opl_lazy_gui = opl_lazy_box.isSelected();
                    opl_prover_filter_subsumed = opl_selfsub_box.isSelected();
                    // simplify = opl_simplify_box.isSelected();
                    opl_prover_compose = opl_compose_box.isSelected();
                    opl_cache_gui = opl_cache_box.isSelected();
                    opl_prover_force_prec = opl_prec_box.isSelected();
                    opl_require_consistency = opl_consistency_box.isSelected();
                    opl_desugar_nat = opl_nat_sugar_box.isSelected();
                    opl_print_simplified_presentations = opl_print_simpl_pres_box.isSelected();
                    opl_display_fresh_ids = opl_fresh_ids_box.isSelected();
                    opl_prover_simplify_instances = opl_simplify_box.isSelected();
                    opl_safe_java = opl_safejava_box.isSelected();
                    opl_secret_agg = opl_agg_box.isSelected();
                    return new Unit();
                };

		return new Pair<>(oplsplit, fn);
	}

	@Override
	public int size() {
		return getClass().getFields().length-1;
	}

}
