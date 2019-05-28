package catdata.opl;

import catdata.Environment;
import catdata.Program;
import catdata.opl.OplExp.OplInst;

class OplInACan {

	public static void main(String... args) {
		System.out.println(openCan(args[0]));
	}
	
	private static String openCan(String can) {
		try {
			Program<OplExp> program = OplParser.program(can);
			Environment<OplObject> result = OplDriver.makeEnv(null, program,
					null, null, null, null);
			String html = "<html><head><title>Result</title></head><body>\n\n";
			for (String definition : program.order) {
				OplObject exp = result.get(definition);
				if (!(exp instanceof OplInst)) { //only show instances
					continue;
				}
				html += "<p><h2>" + definition + " =\n</h2>" + result.get(definition).toHtml()
						+ "\n</p><br><hr>\n";
			}
			return html + "\n\n</body></html>";
		} catch (RuntimeException ex) {
			ex.printStackTrace();
			return "ERROR " + ex.getMessage();
		}
	}

}
