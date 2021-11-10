import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GenerateSource {

	public static void main(String[] args) throws Exception {
        int size = args.length > 0 ? Integer.parseInt(args[0]) : 100;
		List<String> stereotypes = List.of("entity", "repository", "service", "controller");
		Pattern pattern = Pattern.compile("0000");
		for (int i = 1; i < size; i++) {
			String index = String.format("%04d", i);
			for (String stereotype : stereotypes) {
				String suffix = stereotype.substring(0, 1).toUpperCase() + stereotype.substring(1);
				if (stereotype.equals("entity")) {
					suffix = "";
				}
				String content = Files
						.readString(Path
								.of("src/main/java/com/example/demo/" + stereotype + "/Task0000" + suffix + ".java"));
				Matcher matcher = pattern.matcher(content);
				content = matcher.replaceAll(index);
				Files.writeString(
						Path.of("src/main/java/com/example/demo/" + stereotype + "/Task" + index + suffix + ".java"),
						content);
			}
		}
	}
}
