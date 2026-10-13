import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Rename {

    private static final String[] MONTHS = {
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sept", "Oct", "Nov", "Dec"
    };

    public static void main(String[] args) {

        try (Scanner scanner = new Scanner(System.in)) {

            System.out.print("Enter folder path: ");
            Path folder = Paths.get(scanner.nextLine().trim());

            if (!Files.isDirectory(folder)) {
                System.out.println("Invalid folder.");
                return;
            }

            DateTimeFormatter timeFormat =
                    DateTimeFormatter.ofPattern("hh-mm-ssa", Locale.US);

            List<Path> files = new ArrayList<>();

            try (DirectoryStream<Path> stream =
                         Files.newDirectoryStream(folder)) {

                for (Path file : stream) {
                    if (Files.isRegularFile(file)
                            && !Files.isHidden(file)
                            && !file.getFileName().toString().startsWith(".")) {
                        files.add(file);
                    }
                }

                if (files.isEmpty()) {
                    System.out.println("No files found.");
                    return;
                }

                // Sort for consistent processing order
                files.sort(Comparator.comparing(Path::toString));

                System.out.println("\nPreview:");

                List<Path> targets = new ArrayList<>();
                Set<Path> usedTargets = new HashSet<>();

                for (Path file : files) {

                    BasicFileAttributes attributes =
                        Files.readAttributes(
                            file, BasicFileAttributes.class
                        );

                    // Use the earlier of creation and modified time
                    FileTime created  = attributes.creationTime();
                    FileTime modified = attributes.lastModifiedTime();
                    FileTime chosen   =
                        created.compareTo(modified) <= 0 ? created : modified;

                    LocalDateTime date = chosen
                        .toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();

                    String timestamp =
                        String.format("%02d", date.getDayOfMonth()) +
                        MONTHS[date.getMonthValue() - 1] +
                        date.getYear() + " " +
                        date.format(timeFormat)
                            .toLowerCase(Locale.US);

                    String original = file.getFileName().toString();

                    int dot = original.lastIndexOf('.');
                    String extension = dot > 0
                        ? original.substring(dot)
                        : "";

                    String baseName = timestamp;
                    Path target = folder.resolve(baseName + extension);

                    int counter = 2;

                    while ((Files.exists(target) &&
                            !target.equals(file)) ||
                            usedTargets.contains(target)) {

                        target = folder.resolve(
                            baseName + " (" + counter + ")" + extension
                        );
                        counter++;
                    }

                    targets.add(target);
                    usedTargets.add(target);

                    System.out.println(
                        original + " -> " + target.getFileName()
                    );
                }

                System.out.print("\nRename these files? (yes/no): ");

                if (!scanner.nextLine().trim()
                        .equalsIgnoreCase("yes")) {
                    System.out.println("Cancelled.");
                    return;
                }

                for (int i = 0; i < files.size(); i++) {
                    Path source = files.get(i);
                    Path target = targets.get(i);

                    if (source.equals(target)) {
                        continue;
                    }

                    try {
                        Files.move(source, target);
                        System.out.println("Renamed: " +
                            target.getFileName());
                    } catch (IOException e) {
                        System.out.println("Failed: " +
                            source.getFileName() + " - " + e.getMessage());
                    }
                }

                System.out.println("\nFinished.");

            } catch (IOException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }
}