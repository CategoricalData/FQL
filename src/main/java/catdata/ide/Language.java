package catdata.ide;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import catdata.fpql.XCodeEditor;
import catdata.fql.gui.FqlCodeEditor;
import catdata.fqlpp.FQLPPCodeEditor;
import catdata.opl.OplCodeEditor;

public enum Language {

	OPL, FPQL,
	FQL,
	FQLPP;
	
	public static Language getDefault() {
		return FQL;
	}
	
	//non easik ones
	public static Language[] values0() {
		List<Language> l = new LinkedList<>(Arrays.asList(values()));
		//l.remove(EASIK);
		return l.toArray(new Language[0]);
	}
	
	@Override
	public String toString() {
		switch (this) {
		case FQL: return "FQL";
		case FQLPP: return "FQL++";
		case OPL: return "OPL";
		case FPQL: return "FPQL";
		default:
			break;
		}
		throw new RuntimeException("Anomaly - please report");
	}
	
	public String prefix() {
		switch (this) {
		case FQL: return "-";
		case FQLPP: return "+";
		case OPL: return "O";
		case FPQL: return "P";
		default:
			break;
		}
		throw new RuntimeException("Anomaly - please report");
	}

	public String fileExtension() {
		switch (this) {
			case FQL: return "fql";
			case FQLPP: return "fqlpp";
			case OPL: return "opl";
			case FPQL: return "fpql";
			default:
				throw new RuntimeException("Anomaly - please report");
		}

	}

	public String filePath() {
		switch (this) {
			case FQL: return "fql";
			case FQLPP: return "fqlpp";
			case OPL: return "opl";
			case FPQL: return "fpql";
			default:
				throw new RuntimeException("Anomaly - please report");
		}

	}
	
	@SuppressWarnings({ "rawtypes" })
	public CodeEditor createEditor(String title, int id, String content) {
		switch (this) {
		case FPQL: return new XCodeEditor(title, id, content);
		case FQLPP: return new FQLPPCodeEditor(title, id, content);
		case OPL: return new OplCodeEditor(title, id, content);
		case FQL: return new FqlCodeEditor(title,id, content);
		//case MPL: return new MplCodeEditor(title, id, content);
		default:
			throw new RuntimeException("Anomaly - please report");
		}
		
	}

	
	public List<Example> getExamples() {
		switch (this) {
		case FPQL: return Examples.getExamples(Language.FPQL);
		case FQLPP: return Examples.getExamples(Language.FQLPP);
		case OPL: return Examples.getExamples(Language.OPL);
		case FQL: return Examples.getExamples(Language.FQL);
	//	case MPL: return Examples.getExamples(Language.MPL);
		default:
			throw new RuntimeException("Anomaly - please report");
		}
		
	}
}
