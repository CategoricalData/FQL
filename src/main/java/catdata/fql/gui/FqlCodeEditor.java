package catdata.fql.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.sql.Date;
import java.text.DateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;

import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.autocomplete.DefaultCompletionProvider;
import org.fife.ui.autocomplete.ShorthandCompletion;
import org.jparsec.error.ParserException;

import catdata.Pair;
import catdata.Triple;
import catdata.fql.FQLException;
import catdata.fql.decl.Driver;
import catdata.fql.decl.FQLProgram;
import catdata.fql.decl.FqlEnvironment;
import catdata.fql.decl.InstExp;
import catdata.fql.decl.InstanceEditor;
import catdata.fql.decl.MapExp;
import catdata.fql.decl.TransExp;
import catdata.fql.decl.TransExp.Const;
import catdata.fql.decl.TransformEditor;
import catdata.fql.decl.Type;
import catdata.fql.parse.FQLParser;
import catdata.fql.parse.PrettyPrinter;
import catdata.ide.CodeEditor;
import catdata.ide.Language;

/**
 *  
 * @author ryan
 * 
 *         The FQL code editor
 */
@SuppressWarnings("serial")
public class FqlCodeEditor extends CodeEditor<FQLProgram, FqlEnvironment, FqlDisplay> {
	
	
	public FqlCodeEditor(String title, int id, String content) {
		super(title, id, content, new GridLayout(1,1));
		
		JMenuItem visualEdit = new JMenuItem("Visual Edit");
		visualEdit.addActionListener((ActionEvent e) -> vedit());
		topArea.getPopupMenu().add(visualEdit, 0);
		
		
	}

	@Override
	public Language lang() {
		return Language.FQL;
	}

	@Override
	protected String getATMFlhs() {
		return "text/" + Language.FQL.name();
	}

	@Override
	protected String getATMFrhs() {
		return "catdata.fql.parse.FqlTokenMaker";
	}

	@Override
	protected void doTemplates() {
		  CompletionProvider provider = createCompletionProvider();
		  AutoCompletion ac = new AutoCompletion(provider);
		  KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, InputEvent.META_DOWN_MASK
            | InputEvent.SHIFT_DOWN_MASK);
		  ac.setTriggerKey(key);
	      ac.install(topArea);
	}
	
	  private static CompletionProvider createCompletionProvider() {
		   DefaultCompletionProvider provider = new DefaultCompletionProvider();
	
		provider.addCompletion(new ShorthandCompletion(provider,  "schema",
				"schema   = {\n\tnodes;\n\tattributes;\n\tarrows;\n\tequations;\n}", ""));

		provider.addCompletion(new ShorthandCompletion(provider, "mapping",
				"mapping   = {\n\tnodes;\n\tattributes;\n\tarrows;\n} :  -> ", ""));

		provider.addCompletion(new ShorthandCompletion(provider, "instance",
				"instance   = {\n\tnodes;\n\tattributes;\n\tarrows;\n} :  ", ""));

		provider.addCompletion(new ShorthandCompletion(provider,  "query", "query   = delta pi sigma", ""));

		provider.addCompletion(new ShorthandCompletion(provider,  "QUERY",
				"QUERY   = match {} src dst \"deta sigma forward\" ", ""));

		provider.addCompletion(new ShorthandCompletion(provider,  "transform",
				"transform   = {\n\tnodes;\n} :  -> ", "")); 
		
		return provider;
		
	}

	@Override
	public FQLProgram parse(String program) throws ParserException {
		return FQLParser.program(program);
	}

	@Override
	protected FqlDisplay makeDisplay(String foo, FQLProgram init, FqlEnvironment env, long start, long middle) {
		return new FqlDisplay(foo, init, env, start, middle);
	}

	private final Map<FqlEnvironment, String> textForCache = new HashMap<>();
	@Override
	protected FqlEnvironment makeEnv(String str, FQLProgram init) {
		Triple<FqlEnvironment, String, List<Throwable>> envX = Driver
				.makeEnv(init, toUpdate);
		if (envX.third.isEmpty()) {
			textForCache.put(envX.first, envX.second);
			return envX.first;
		} 
			String ret = "";
			for (Throwable t : envX.third) {
				ret += t.getMessage() + "\n----------------------\n";
				t.printStackTrace();
			}
			throw new RuntimeException(ret);
		
	}

	@Override
	protected String textFor(FqlEnvironment env) {
		if (!textForCache.containsKey(env)) {
			throw new RuntimeException();
		}
		return textForCache.get(env);
	} 
	

	public void vedit() {
		FQLProgram init = tryParse(topArea.getText());
		if (init == null) {
			respArea.setText(toDisplay);
			return;
		}
		if (init.lines.isEmpty()) {
			return;
		}
		String which = null;
		int start = -1;
		int offs = topArea.getCaretPosition();
		int end = -1;
		int i = 0;
		int pos = 0;
		for (String k : init.lines.keySet()) {
			Integer v = init.lines.get(k);
			if (v < offs && v > start) {
				start = v;
				which = k;
				pos = i;
			}
			i++;
		}
		if (which == null) {
			throw new RuntimeException();
		}

		int j = 0;
		for (String k : init.lines.keySet()) {
			if (j == pos + 1) {
				end = init.lines.get(k);
				break;
			}
			j++;
		}
		if (end == -1) {
			end = topArea.getText().length();
		}

		InstExp ie = init.insts.get(which);
		TransExp te = init.transforms.get(which);
		if ( (ie == null && te == null) 
		   ||(ie != null && !(ie instanceof InstExp.Const))
		   ||(te != null && !(te instanceof Const)) ) {
			respArea.setText("Cannot visually edit "
					+ which
					+ ": only constant instances or transforms are visually editable.");
			return;
		}
		try {
			if (ie != null) {
				InstExp.Const iec = (InstExp.Const) ie;
				InstExp.Const n = new InstanceEditor(which, iec.sig
						.toSig(init), iec).show(Color.black);
				if (n == null) {
					return;
				}
				String newText = "instance " + which + " = " + n
						+ " : " + n.sig + "\n\n";
				topArea.replaceRange(newText, start, end);
			} else {
				Const iec = (Const) te;
				if (iec == null) {
					throw new RuntimeException("Anomaly: please report");
				}
				InstExp.Const s = (InstExp.Const) init.insts.get(iec.src);
				InstExp.Const t = (InstExp.Const) init.insts.get(iec.dst);
				Const n = new TransformEditor(which, init.insts.get(iec.src).type(init).toSig(init), iec, s, t).show(Color.black);
				if (n == null) {
					return;
				}
				String newText = "transform " + which + " = " + n
						+ " : " + n.src + " -> " + n.dst + "\n\n";
				topArea.replaceRange(newText, start, end);
				
			}
		} catch (FQLException fe) {
			fe.printStackTrace();
			respArea.setText(fe.getLocalizedMessage());
		}

	}

	
	public void format() {
		String input = topArea.getText();
		FQLProgram p = tryParse(input);
		if (p == null) {
			respArea.setText(toDisplay);
			return;
		}
		if (input.contains("//") || input.contains("/*")) {
			int x = JOptionPane.showConfirmDialog(null, "Formatting will erase all comments - continue?", "Continue?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
			if (x != JOptionPane.YES_OPTION) {
				return;
			}
		}
		//order does not contain enums or drops
		StringBuilder sb = new StringBuilder();
		for (String k : p.enums.keySet()) {
			Type t = p.enums.get(k);
			if (!(t instanceof Type.Enum)) {
				continue;
			}
			Type.Enum e = (Type.Enum) t;
			sb.append("enum ").append(k).append(" = ").append(e.printFull());
			sb.append("\n\n");
		}
		for (String k : p.order) {
			Pair<String, Object> o = get(p, k);
			sb.append(o.first).append(" ").append(k).append(" = ").append(o.second);
			if (o.second instanceof InstExp.Const) {
				InstExp.Const c = (InstExp.Const) o.second;
				sb.append(" : ").append(c.sig);
			} else if (o.second instanceof MapExp.Const) {
				MapExp.Const c = (MapExp.Const) o.second;
				sb.append(" : ").append(c.src).append(" -> ").append(c.dst);
			} else if (o.second instanceof Const) {
				Const c = (Const) o.second;
				sb.append(" : ").append(c.src).append(" -> ").append(c.dst);
			}
			sb.append("\n\n");
		}
		if (!p.drop.isEmpty()) {
			sb.append("drop ").append(PrettyPrinter.sep0(" ", p.drop)).append("\n\n");
		}
		topArea.setText(sb.toString().trim());
		topArea.setCaretPosition(0);
	}
	

	private static Pair<String, Object> get(FQLProgram p, String k) {
		Object o = p.full_queries.get(k);

		if (o != null) {
			return new Pair<>("QUERY", o);
		}
		
		o = p.queries.get(k);
		if (o != null) {
			return new Pair<>("query", o);
		}
		
		o = p.insts.get(k);
		if (o != null) {
			return new Pair<>("instance", o);
		}
		
		o = p.maps.get(k);
		if (o != null) {
			return new Pair<>("mapping", o);
		}
		
		o = p.sigs.get(k);
		if (o != null) {
			return new Pair<>("schema", o);
		}
		
		o = p.transforms.get(k);
		if (o != null) {
			return new Pair<>("transform", o);
		}
		
		throw new RuntimeException("Cannot find " + k);
	} 
	
	public void check() {
		String program = topArea.getText();

		FQLProgram init;
		try {
			init = FQLParser.program(program);
		} catch (ParserException e) {
			int col = e.getLocation().column;
			int line = e.getLocation().line;
			topArea.requestFocusInWindow();
			topArea.setCaretPosition(topArea.getDocument()
					.getDefaultRootElement().getElement(line - 1)
					.getStartOffset()
					+ (col - 1));
			//String s = e.getMessage();
			//String t = s.substring(s.indexOf(" "));
			//t.split("\\s+");

			respArea.setText("Syntax error: " + e.getLocalizedMessage());
			e.printStackTrace();
			return;
		} catch (RuntimeException e) {
			respArea.setText("Error: " + e.getLocalizedMessage());
			e.printStackTrace();
			return;
		}

		String xxx = Driver.checkReport(init);

		DateFormat format = DateFormat.getTimeInstance();
		String time = format.format(new Date(System.currentTimeMillis()));
		String foo = title;

		JTextArea jta = new JTextArea(xxx);
		jta.setWrapStyleWord(true);
		jta.setLineWrap(true);
		JScrollPane p = new JScrollPane(jta,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		p.setPreferredSize(new Dimension(650, 300));

		JOptionPane pane = new JOptionPane(p);
		JDialog dialog = pane.createDialog(null, "Type Check " + foo + " - "
				+ time);
		dialog.setModal(false);
		dialog.setResizable(true);
		dialog.setVisible(true);

	}

}
