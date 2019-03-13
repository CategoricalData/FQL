package catdata.fqlpp;

import java.awt.GridLayout;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.autocomplete.DefaultCompletionProvider;
import org.fife.ui.autocomplete.ShorthandCompletion;
import org.jparsec.error.ParserException;

import catdata.ide.CodeEditor;
import catdata.ide.Language;

@SuppressWarnings("serial")
public class FQLPPCodeEditor extends CodeEditor<FQLPPProgram, FQLPPEnvironment, FqlppDisplay> {

	public FQLPPCodeEditor(String title, int id, String content) {
		super(title, id, content, new GridLayout(1,1));
	}

	@Override
	public Language lang() {
		return Language.FQLPP;
	}

	@Override
	protected String getATMFlhs() {
		return "text/" + Language.FQLPP.name();
	}

	@Override
	protected String getATMFrhs() {
		return "catdata.fqlpp.FqlPPTokenMaker";
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
	
		   provider.addCompletion(new ShorthandCompletion(provider, "set",
	            "set   = { }", ""));
		
		   provider.addCompletion(new ShorthandCompletion(provider, "function",
		            "function   = { } :  -> ", ""));
				
		   provider.addCompletion(new ShorthandCompletion(provider, "category",
		            "category   = {\n\tobjects;\n\tarrows;\n\tequations;\n}", ""));
		   
		   provider.addCompletion(new ShorthandCompletion(provider, "functor",
		            " functor   = {\n\tobjects;\n\tarrows;\n} :  -> ", ""));
		
		   provider.addCompletion(new ShorthandCompletion(provider, "transform",
				   "transform   = {\n\tobjects;\n} : ( :  -> ) -> ( :  -> ) ", ""));
		   
		   return provider;
	}

	@Override
	protected FQLPPEnvironment makeEnv(String program, FQLPPProgram init) {
		return FQLPPDriver.makeEnv(program, init, toUpdate);
	}

	@Override
	public FQLPPProgram parse(String program) throws ParserException {
		return PPParser.program(program);
	}

	@Override
	protected FqlppDisplay makeDisplay(String foo, FQLPPProgram init, FQLPPEnvironment env, long start, long middle) {
		return new FqlppDisplay(foo, init, env);
	}

	@Override
	protected String textFor(FQLPPEnvironment env) {
		return "Done";
	}

}
