package catdata.fpql;

import javax.swing.BorderFactory;
import javax.swing.JComponent;

import catdata.ide.CodeTextPanel;

public class XString implements XObject {

	private final String title;
	private final String value;


	public XString(String title, String value) {
		this.title = title;
		this.value = value;
	}
	
	
	@Override
	public JComponent display() {
		return new CodeTextPanel(BorderFactory.createEtchedBorder(), title, value);
	}


	@Override
	public String kind() {
		return "black-box";
	}

}
