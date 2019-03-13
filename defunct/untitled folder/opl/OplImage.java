package catdata.opl;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import java.net.URL;

public class OplImage implements OplObject {
	
	private String msg;
	private JLabel label;
	private String url;
	
	public OplImage(String url) {
		try {
			this.url = url;
			label = new JLabel(new ImageIcon(new URL(url)));
		} catch (Exception ex) {
			ex.printStackTrace();
			msg = ex.getMessage();
		}
	}

	@Override
	public JComponent display() {
		if (label != null) {
			return label;
		}
		return new JLabel(msg);
	}
	
	@Override
	public String toString() {
		if (msg != null) {
			return msg;
		}
		return url;
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof OplImage)) {
			return false;
		}
		OplImage other = (OplImage) o;
		boolean ret = (other.url.equals(url));
		return ret; 
	}
	
	@Override
	public int hashCode() {
		return 0;
	}

}
