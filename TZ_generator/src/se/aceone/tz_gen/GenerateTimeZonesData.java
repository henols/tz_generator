package se.aceone.tz_gen;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class GenerateTimeZonesData {

	private static final String TZDATA_C = "tzdata.c";

	public static void main(String[] args) throws IOException {

		File inputFile = null;
		File outputDir = null;
		File aliasFile = null;
		boolean verbose = false;
		boolean replaseWithAlias = false;

		int i = 0;
		for (i = 0; i < args.length; i++) {
			try {
				if ("-src".equals(args[i])) {
					inputFile = new File(args[++i]);
				} else if ("-dst".equals(args[i])) {
					outputDir = new File(args[++i]);
				} else if ("-alias".equals(args[i])) {
					aliasFile = new File(args[++i]);
				} else if ("-r".equals(args[i])) {
					replaseWithAlias = true;
					;
				} else if ("-verbose".equals(args[i])) {
					verbose = true;
				} else if ("-?".equals(args[i])) {
					printUsage();
					return;
				} else {
					break;
				}
			} catch (IndexOutOfBoundsException e) {
				printUsage();
				return;
			}
		}

		if (i != args.length) {
			printUsage();
			return;
		}
		if (inputFile == null || outputDir == null) {
			printUsage();
			return;
		}

		BufferedReader reader = new BufferedReader(new FileReader(inputFile));
		DateTimeFormatter fmt = DateTimeFormat.forPattern("MMM dd HH:mm:ss yyyy").withZoneUTC();

		Map<String, ZoneDescriptor> tz = new HashMap<String, ZoneDescriptor>();

		String line;

		String timeZoneName = null;

		boolean firstDayligtSaving = false;
		DateTimeZone zone = null;
		int count = 0;

		int dstOffset = 0;

		long dstStart = 0;

		ZoneDescriptor desc = null;

		while ((line = reader.readLine()) != null) {
			// Pacific/Pago_Pago Wed Nov 30 10:59:59 1983 UT = Tue Nov 29 23:59:59 1983 BST isdst=0 gmtoff=-39600
			String[] s = line.split("\\s+");

			if (!s[0].equals(timeZoneName)) {
				timeZoneName = s[0];
				firstDayligtSaving = false;
				zone = DateTimeZone.forID(timeZoneName);
				count = 0;
			}

			String dateTime = s[2] + " " + s[3] + " " + s[4] + " " + s[5];
			DateTime dt = fmt.parseDateTime(dateTime);

			if (dt.getYear() == 2016 && s[14].equals("isdst=1") && dt.getSecondOfMinute() == 0) {
				firstDayligtSaving = true;
				// System.out.println(" --------------" + zone.getName(0) + "----------------- ");
				// System.out.println(timeZoneName+" "+ zone.getStandardOffset(0)+" "+zone.getName(0)+" "+
				// zone.getShortName(0));
				desc = new ZoneDescriptor(timeZoneName, zone.getName(0));
				tz.put(desc.getTimeZoneName(), desc);
			}
			if (dt.getYear() > 2016 && !tz.containsKey(timeZoneName)) {
				ZoneDescriptor d = new ZoneDescriptor(timeZoneName, zone.getName(0));
				int standardOffset = Integer.parseInt(s[15].substring(7));
				d.setOffset(standardOffset);
				d.setShortName(s[13]);
				tz.put(timeZoneName, d);
			}

			if (firstDayligtSaving && s[14].equals("isdst=0")) {
				// System.out.println(line);
			}

			if (firstDayligtSaving && dt.getSecondOfMinute() == 0) {
				if (count < 20) {
					boolean dst = (count % 2) == 0;
					// System.out.println(dateTime + " == " + (dst ? "DST" : "Normal time") + " " + s[14]);
					// System.out.println(line + " ======= " + "" + dt.toString() + " " + zone.getOffset(0));
					if (dst) {
						dstOffset = Integer.parseInt(s[15].substring(7));
						dstStart = dt.getMillis() / 1000;

					} else {
						int standardOffset = Integer.parseInt(s[15].substring(7));
						// System.out.println("DST offset:" + (dstOffset - standardOffset));
						desc.setDstOffset(dstOffset - standardOffset);
						desc.setOffset(standardOffset);
						desc.setShortName(s[13]);
						desc.addDst((int) (dstStart), (int) (dt.getMillis() / 1000));
					}
					count++;
				} else {
					if (desc != null) {
						firstDayligtSaving = false;
						// System.out.println(line);

						desc = null;
					}
				}

				// for(int i = 0;i < split.length; i++){
				// System.out.println("'"+split[i]+"'");
			}
			// break;
		}
		reader.close();

		if (aliasFile != null) {
			BufferedReader aliases = new BufferedReader(new FileReader(aliasFile));
			while ((line = aliases.readLine()) != null) {
				String[] alias = line.split(",");
				if (alias.length == 2 && tz.containsKey(alias[1])) {
					System.out.println("Adding alias: " + alias[0] + " for TZ: " + alias[1]);
					tz.put(alias[0], tz.get(alias[1]).copy(alias[0]));
					if (replaseWithAlias) {
						tz.remove(alias[1]);
					}
				}
			}
			aliases.close();
		}

		List<String> tzNames = new ArrayList<String>();
		List<String> shortNames = new ArrayList<String>();
		List<ZoneDescriptor> zdDesc = new ArrayList<ZoneDescriptor>();
		List<DSTDescriptor> dstDesc = new ArrayList<DSTDescriptor>();

		TreeBuilder treeBuilder = new TreeBuilder();

		for (ZoneDescriptor zd : tz.values()) {
			System.out.println("------------");
			System.out.println(zd);
			// zd.write(outputDir);
			if (!tzNames.contains(zd.getName())) {
				tzNames.add(zd.getName());
			}
			if (!shortNames.contains(zd.getShortName())) {
				shortNames.add(zd.getShortName());
			}

			if (!zdDesc.contains(zd)) {
				zdDesc.add(zd);
			}

			if (!dstDesc.contains(zd.getDSTDescriptor())) {
				dstDesc.add(zd.getDSTDescriptor());
			}
			treeBuilder.add(zd.getTimeZoneName(), zd);

		}

		System.out.println("------------");
		System.out.println("Number of zones: " + tz.size());

		System.out.println("------------");
		System.out.println("Number of zones names : " + tzNames.size());

		System.out.println("------------");
		System.out.println("Number of short names : " + shortNames.size());
		// for (String tzName : tzNames) {
		// System.out.println(tzName);
		// }
		System.out.println("------------");
		System.out.println("Number of zones : " + zdDesc.size());

		int ztCount = 0;

		for (ZoneDescriptor dstDescriptor : zdDesc) {
			// System.out.println("------------");
			// System.out.println(dstDescriptor);
			// dstDescriptor.print(System.out);
			if (!dstDescriptor.getDst().isEmpty()) {
				ztCount++;
			}
		}
		System.out.println("------------");
		System.out.println("Number of different TZ with DST: " + ztCount);
		System.out.println("Number of different TZ         : " + (zdDesc.size() - ztCount));

		ztCount = 0;
		for (DSTDescriptor dstDescriptor : dstDesc) {
			// System.out.println("------------");
			// System.out.println(dstDescriptor);
			if (!dstDescriptor.getDst().isEmpty()) {
				ztCount++;
			}
		}
		System.out.println("------------");
		System.out.println("Number of different TZ with DST: " + ztCount);
		System.out.println("Number of different TZ         : " + (dstDesc.size() - ztCount));

		generateCFiles(outputDir, tz, zdDesc, dstDesc, treeBuilder, shortNames, args);

		System.out.println("TZ files are generated");

	}

	protected static void generateCFiles(File outDir, Map<String, ZoneDescriptor> tz, List<ZoneDescriptor> zdDesc, List<DSTDescriptor> dstDesc,
			TreeBuilder treeBuilder, List<String> shortNames, String[] args) throws FileNotFoundException {
		File sourceFile = new File(outDir, TZDATA_C);
		// File headerFile = new File(outDir, "tzdata.h");

		PrintStream source = new PrintStream(sourceFile);
		// PrintStream header = new PrintStream(headerFile);
		//
		// header.println("#ifndef TZDATA_H_");
		// header.println("#define TZDATA_H_");
		// header.println("#ifdef __cplusplus");
		// header.println("extern \"C\" {");
		// header.println("#endif");

		LocalDate localDate = new LocalDate();
		source.println("/*");
		source.println(" * © henols@gmail.com");
		source.println(" *");
		source.println(" * " + TZDATA_C);
		source.println(" *");
		source.println(" *  Created on: " + localDate.toString());
		source.println(" *  Generated by: " + GenerateTimeZonesData.class.getName());
		source.println(" *");
		for (int i = 0; i < args.length; i++) {
			source.print(" *    " + args[i]);
			if (args[i].charAt(0) == '-' && i + 1 < args.length && args[i + 1].charAt(i + 1) != '-') {
				source.print(" " + args[++i]);
			}
			source.println();
		}
		source.println(" */");
		source.println();

		source.println("#include \"time_tools/localtime.h\"");

		source.println();
		source.println("#include <stddef.h>");
		source.println("#include <stdlib.h>");
		source.println("#include <string.h>");

		source.println();
		List<TreeBuilder> childList = treeBuilder.getAllChildren();
		for (TreeBuilder tb : childList) {
			String name = tb.getName();
			if (!name.equalsIgnoreCase("INDIANA") && !name.equalsIgnoreCase("NORTH_DAKOTA") && !name.equalsIgnoreCase("KENTUCKY")) {
				source.println("#define " + name.toUpperCase());
			} else {
				source.println("// #define " + name.toUpperCase());
			}
		}
		source.println();
		for (TreeBuilder tb : childList) {
			source.println("#ifdef " + tb.getName().toUpperCase());
			source.println("static int " + tb.getMethodName() + "(zone_info_t * tz_info, char * key);");
			source.println("#endif // " + tb.getName().toUpperCase());
		}
		source.println();

		for (ZoneDescriptor zd : zdDesc) {
			ZoneDescriptor dd = dstDesc.get(dstDesc.indexOf(zd.getDSTDescriptor())).getZoneDescriptor();
			source.println("#define " + zd.getDefineByteName() + " \t" + dd.getVarByteName());
			source.println("#define " + zd.getDefineSizeName() + " \t" + dd.getVarSizeName());
		}
		source.println();

		for (ZoneDescriptor zd : tz.values()) {
			ZoneDescriptor dd = zdDesc.get(zdDesc.indexOf(zd));
			source.println("#define " + zd.getDefineByteTimeZone() + " \t" + dd.getDefineByteName());
			source.println("#define " + zd.getDefineSizeTimeZone() + " \t" + dd.getDefineSizeName());
		}

		source.println();
		for (String s : shortNames) {
			s = ZoneDescriptor.toSafeName(s);
			source.println("#define " + s.toUpperCase() + " \ttz_short_name_" + s.toLowerCase());
		}

		source.println();
		for (DSTDescriptor dstDescriptor : dstDesc) {
			dstDescriptor.getZoneDescriptor().print(source);
		}

		source.println();
		for (String s : shortNames) {
			source.print("static const char tz_short_name_" + ZoneDescriptor.toSafeName(s).toLowerCase() + "[] = {");
			byte[] bytes = s.getBytes();
			for (byte b : bytes) {
				source.print(b + ", ");
			}
			source.println("};");
		}
		source.println();
		source.println("static int build_zone_info(zone_info_t * tz_info, const char * info, int info_len, const char * name, int name_len){");
		source.println("  int rc;");
		source.println("  char * buf = (char*) vm_malloc(4 + info_len);");
		source.println("  memset(buf, 0, 4 + info_len);");
		source.println("  memcpy(buf, name, name_len);");
		source.println("  memcpy(&buf[4], info, info_len);");
		source.println("  rc = tt_time_build_zone_info(tz_info, buf, 4 + info_len);");
		source.println("  vm_free(buf);");
		source.println("  return rc;");
		source.println("};");

		source.println();
		if (treeBuilder.getName() != null) {
			source.println("#ifdef " + treeBuilder.getName().toUpperCase());
		}
		source.println("int tt_time_" + treeBuilder.getMethodName() + "(zone_info_t * tz_info, char * key){");
		buildTzSelection(source, treeBuilder);
		source.println("  return NULL;");
		source.println("}");
		if (treeBuilder.getName() != null) {
			source.println("#endif // " + treeBuilder.getName().toUpperCase());
		}
		source.println();

		for (TreeBuilder tb : childList) {
			if (tb.getName() != null) {
				source.println("#ifdef " + tb.getName().toUpperCase());
			}
			source.println("static int " + tb.getMethodName() + "(zone_info_t * tz_info, char * key){");
			buildTzSelection(source, tb);

			source.println("  return 0;");
			source.println("}");
			if (tb.getName() != null) {
				source.println("#endif // " + tb.getName().toUpperCase());
			}
			source.println();
		}

		// header.println("#ifdef __cplusplus");
		// header.println("}");
		// header.println("#endif");

		// header.println("#endif /* */");

		source.close();
		// header.close();

	}

	protected static void buildTzSelection(PrintStream source, TreeBuilder tb) {

		source.println("  char * pos = strchr(key, '/');");

		// source.println(" vm_log_info(\" Key in "+ tb.getMethodName() +": %s\",key);");

		source.println("  if(pos > key) {");
		for (TreeBuilder c : tb.getTreeBuilderChildren()) {
			if (c.getName() != null) {
				source.println("#ifdef " + c.getName().toUpperCase());
			}
			source.println("    if(strncmp(key, \"" + c.getName() + "\", pos - key) == 0) {");
			source.println("      return " + c.getMethodName() + "(tz_info, pos + 1);");
			source.println("    }");
			if (c.getName() != null) {
				source.println("#endif // " + c.getName().toUpperCase());
			}
		}
		source.println("  } else {");
		for (ZoneDescriptor zd : tb.getZoneDescChildren()) {
			int ind = zd.getTimeZoneName().lastIndexOf('/');
			String name = zd.getTimeZoneName();
			if (ind >= 0) {
				name = zd.getTimeZoneName().substring(ind + 1);
			}
			source.println("    if(strcmp(key, \"" + name + "\") == 0) {");

			source.println("      return build_zone_info(tz_info, " + zd.getDefineByteTimeZone() + ", " + zd.getDefineSizeTimeZone() + ", "
					+ zd.getDefineShortName() + ", " + zd.getShortName().length() + ");");
			// source.println(" return tt_time_build_zone_info(tz_info, " + zd.getDefineByteTimeZone() + ", " +
			// zd.getDefineSizeTimeZone() + ");");
			source.println("    }");
		}
		source.println("  }");
	}

	// PST8PDT Sun Mar 14 09:59:59 2021 UT = Sun Mar 14 01:59:59 2021 PST isdst=0 gmtoff=-28800 ======= 17
	// PST8PDT Sun Mar 14 10:00:00 2021 UT = Sun Mar 14 03:00:00 2021 PDT isdst=1 gmtoff=-25200 ======= 17
	// PST8PDT Sun Nov 7 08:59:59 2021 UT = Sun Nov 7 01:59:59 2021 PDT isdst=1 gmtoff=-25200 ======= 19
	// PST8PDT Sun Nov 7 09:00:00 2021 UT = Sun Nov 7 01:00:00 2021 PST isdst=0 gmtoff=-28800 ======= 19

	private static void printUsage() {
		System.out.println("Usage: java " + GenerateTimeZonesData.class.getName() + " <options>");
		System.out.println("where possible options include:");
		System.out.println("  -src <file>         Specify where to read source file");
		System.out.println("  -alias <file>         Specify where to read alias file");
		System.out.println("  -dst <directory>    Specify where to write generated files");
		System.out.println("  -verbose            Output verbosely (default false)");
	}

}