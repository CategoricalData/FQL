package catdata.opl;

import javax.swing.JComponent;

@FunctionalInterface
public interface OplObject {

	JComponent display();

	default String toHtml() {
		return toString().replace("\n", "<br>").replace("\t", "&nbsp;");
	}
	
	
}
