package catdata.opl;

import java.awt.GridLayout;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.autocomplete.DefaultCompletionProvider;
import org.fife.ui.autocomplete.ShorthandCompletion;
import org.jparsec.error.ParserException;

import catdata.Environment;
import catdata.Program;
import catdata.ide.CodeEditor;
import catdata.ide.DefunctGlobalOptions;
import catdata.ide.Language;

@SuppressWarnings("serial")
public class OplCodeEditor extends
		CodeEditor<Program<OplExp>, Environment<OplObject>, OplDisplay> {

	public OplCodeEditor(String title, int id, String content) {
		super(title, id, content, new GridLayout(1,1));
	}

	@Override
	public Language lang() {
		return Language.OPL;
	}

	@Override
	protected String getATMFlhs() {
		return "text/" + Language.OPL.name();
	}

	@Override
	protected String getATMFrhs() {
		return "catdata.opl.OplTokenMaker";
	}

	@Override
	protected void doTemplates() {
		CompletionProvider provider = createCompletionProvider();
		AutoCompletion ac = new AutoCompletion(provider);
		KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_SPACE,
				InputEvent.META_DOWN_MASK
						| InputEvent.SHIFT_DOWN_MASK);
		ac.setTriggerKey(key);
		ac.install(topArea);
	}

	private static CompletionProvider createCompletionProvider() {
		DefaultCompletionProvider provider = new DefaultCompletionProvider();

		provider.addCompletion(new ShorthandCompletion(provider, "theory",
				"theory {\n\tsorts;\n\tsymbols;\n\tequations;\n}", ""));

		provider.addCompletion(new ShorthandCompletion(
				provider,
				"SCHEMA",
				"SCHEMA {\n\tentities;\n\tedges;\n\tattributes;\n\tpathEqualities;\n\tobsEqualities;\n} : ",
				""));

		provider.addCompletion(new ShorthandCompletion(provider, "model",
				"model {\n\tsorts;\n\tsymbols;\n} : ", ""));

		provider.addCompletion(new ShorthandCompletion(provider, "query",
				"query {\n q1 = "
				+ "\n {for a:b; "
				+ "\n  where f(a)=f(b), f(b)=f(c); "
				+ "\n  return att = at(a), att2 = at(a); "
				+ "\n  keys fk1 = {a=f(b), b=f(g(a))} : q2,"
				+ "\n  fk2 = {c=f(b)} : q3; "
				+ " } : t \n/* , another block */ \n} : S -> T ", ""));
	
		provider.addCompletion(new ShorthandCompletion(provider, "sql",
				"sql {\n  "
				+ "\n  insert into A "
				+ "\n  select (f(a) as a, f(a) as b) as a,"
				+ "\n         f(x) as x"
				+ "\n  from A as a "
				+ "\n  where f(a)=f(b) and f(b)=f(c) "
				+ "\n  /* , another block */ \n} : S -> T ", ""));

		provider.addCompletion(new ShorthandCompletion(provider, "javascript",
				"javascript {\n\tsymbols;\n} : ", ""));

		provider.addCompletion(new ShorthandCompletion(provider, "mapping",
				"mapping {\n\tsorts;\n\tsymbols;\n} :  -> ", ""));

		provider.addCompletion(new ShorthandCompletion(provider, "transform",
				"tranform {\n\tsorts;\n} :  ->  ", ""));

		provider.addCompletion(new ShorthandCompletion(provider, "transpres",
				"transpres {\n\tsorts;\n} :  ->  ", ""));

		provider.addCompletion(new ShorthandCompletion(provider,
				"presentation",
				"presentation {\n\tgenerators;\n\tequations;\n} : ", ""));

		provider.addCompletion(new ShorthandCompletion(provider, "INSTANCE",
				"INSTANCE {\n\tgenerators;\n\tequations;\n} : ", ""));
		
		provider.addCompletion(new ShorthandCompletion(provider, "graph",
				"graph {\n\tnodes;\n\tedges;\n} ", ""));
		
		provider.addCompletion(new ShorthandCompletion(provider, "tables",
				"tables {\n\tentities;\n\tedges;\n\tattributes;} :  ", ""));
		
		provider.addCompletion(new ShorthandCompletion(provider, "colimit",
				"colimit typeSideOrSchema graph ", ""));
		return provider;

	}

	@Override
	public Program<OplExp> parse(String program) throws ParserException {
		return OplParser.program(program);
	}

	@Override
	protected OplDisplay makeDisplay(String foo, Program<OplExp> init,
			Environment<OplObject> env, long start, long middle) {
		try {
			OplDisplay ret = new OplDisplay(foo, init, env, start, middle);
			DefunctGlobalOptions.debug.opl = last_options;
			return ret;
		} catch (RuntimeException ex) {
			if (last_options != null) {
				DefunctGlobalOptions.debug.opl = last_options;
			}
			throw ex;
		}
	}

	private String last_str;
	private Program<OplExp> last_prog;
	private Environment<OplObject> last_env;

	@Override
	protected Environment<OplObject> makeEnv(String str, Program<OplExp> init) {
		last_options = (OplOptions) DefunctGlobalOptions.debug.opl.clone();
		try {
			last_env = OplDriver.makeEnv(str, init, toUpdate, last_str,
					last_prog, last_env);
			last_prog = init;
			last_str = str;
			return last_env;
		} catch (Exception ex) {
			if (last_options != null) {
				DefunctGlobalOptions.debug.opl = last_options;
			}
			throw ex;
		}
	}

	private OplOptions last_options;

	@Override
	protected String textFor(Environment<OplObject> env) {
		return "Done.";
	}

}
