package catdata.opl;

import javax.swing.BorderFactory;
import javax.swing.JComponent;

import catdata.ide.CodeTextPanel;

public class OplString implements OplObject {
	private final String str;

	public OplString(String str) {
		this.str = str;
	}

	@Override
	public String toString() {
		return OplTerm.strip(str);
	}

	@Override
	public JComponent display() {
		return new CodeTextPanel(BorderFactory.createEtchedBorder(), "", str);
	}

}