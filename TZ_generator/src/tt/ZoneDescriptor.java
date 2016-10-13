package tt;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

class ZoneDescriptor {
	private String timeZoneName;
	private int offset;
	private String shortName;
	private int dstOffset = -1;
	private String name;

	private List<int[]> dst = new ArrayList<int[]>();
	private DSTDescriptor dstDescriptor;

	ZoneDescriptor(String timeZoneName, String name) {
		this.timeZoneName = timeZoneName;
		this.name = name;
		dstDescriptor = new DSTDescriptor(this);
	}

	public ZoneDescriptor copy(String timeZoneName) {
		ZoneDescriptor c = new ZoneDescriptor(timeZoneName, this.name);
		c.offset = offset;
		c.shortName = shortName;
		c.dstOffset = dstOffset;
		c.dst = dst;
		return c;
	}

	public String getDefineByteTimeZone() {
		return getDefineTimeZone() + "_NAME";
	}

	public String getDefineSizeTimeZone() {
		return getDefineTimeZone() + "_SIZE";
	}

	public String getDefineTimeZone() {
		return toSafeName(timeZoneName).toUpperCase();
	}

	public String getName() {
		return name;
	}

	DSTDescriptor getDSTDescriptor() {
		return dstDescriptor;
	}

	void print(PrintStream out) {

		out.println("// " + timeZoneName + " - " + name + " (" + shortName + ")");
		out.print("static const char " + getVarByteName() + "[] = {");
		int byteCount = 0;
//		int snLen = shortName.length();
//		byteCount = snLen;
//
//		byte[] bytes = shortName.getBytes();
//		for (byte b : bytes) {
//			out.print(b + ", ");
//		}
//		for (; snLen < 4; snLen++) {
//			byteCount++;
//			out.print("0, ");
//		}
		byteCount += printIntBytes(out, offset);
		if (!getDst().isEmpty()) {
			byteCount += printIntBytes(out, dstOffset);
			for (int[] is : getDst()) {
				byteCount += printIntBytes(out, is[0]);
				byteCount += printIntBytes(out, is[1]);
			}
		}
		out.println("};");
		out.println("const int " + getVarSizeName() + " = " + byteCount + ";");
		out.println();
	}

	String getVarSizeName() {
		return "tz_size_" + getGoodVarName();
	}

	String getVarByteName() {
		return "tz_info_" + getGoodVarName();
	}

	String getDefineName() {
		return getGoodVarName().toUpperCase();
	}

	String getDefineByteName() {
		return getDefineName() + "_INFO";
	}

	String getDefineSizeName() {
		return getDefineName() + "_SIZE";
	}

	void write(File destDir) throws IOException {

		// 34 * 8 = 272
		// 40 * 92 = 3680

		if (!destDir.exists() && !destDir.mkdirs()) {
			throw new IOException("Destination directory doesn't exist and cannot be created: " + destDir);
		}
		if (!destDir.isDirectory()) {
			throw new IOException("Destination is not a directory: " + destDir);
		}

		File destFile = new File(destDir, timeZoneName);
		if (!destFile.getParentFile().exists()) {
			destFile.getParentFile().mkdirs();
		}

		DataOutputStream dout = new DataOutputStream(new FileOutputStream(destFile));
		int snLen = shortName.length();

		byte[] bytes = shortName.getBytes();
		dout.write(bytes);
		for (; snLen < 4; snLen++) {
			dout.write(0);
		}
		dout.writeInt(offset);
		if (!getDst().isEmpty()) {
			dout.writeInt(dstOffset);
			for (int[] is : getDst()) {
				dout.writeInt(is[0]);
				dout.writeInt(is[1]);
			}
		}
		dout.close();
	}

	int printIntBytes(PrintStream out, int i) {
		byte[] bytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(i).array();
		for (byte b : bytes) {
			out.print(((int) b & 0xFF) + ", ");
		}
		return bytes.length;
	}

	String getGoodVarName() {
		return toSafeName(timeZoneName + "_" + shortName);
	}

	public void setOffset(int offset) {
		this.offset = offset;
	}

	public int getDstOffset() {
		return dstOffset;
	}

	public void setDstOffset(int dstOffset) {
		this.dstOffset = dstOffset;
	}

	public String getShortName() {
		return shortName;
	}
	public static String toSafeName(String s) {
		return s.replace('/', '_').replace('-', 'M').replace('+', 'P');
	}

	public String getDefineShortName() {
		return toSafeName(getShortName()).toUpperCase();
	}
	
	public void setShortName(String shortName) {
		this.shortName = shortName;
	}

	void addDst(int start, int stop) {
		int s[] = { start, stop };
		getDst().add(s);
	}

	@Override
	public String toString() {
		String s = timeZoneName + ", " + name + " (" + shortName + ") offset:" + offset + " " + ((double) offset / 3600) + "\n";
		if (getDst().size() > 0) {
			s += "\t" + dstOffset + "\n";
			for (int[] is : getDst()) {
				s += "\t" + is[0] + " <-> " + is[1] + "\n";
			}
		}
		return s;
	}

	@Override
	public boolean equals(Object obj) {
		return equals(obj, true);
	}

	public String getTimeZoneName() {
		return timeZoneName;
	}

	public boolean equals(Object obj, boolean full) {
		if (!(obj instanceof ZoneDescriptor)) {
			return false;
		}
		ZoneDescriptor dstd = (ZoneDescriptor) obj;

		if (offset != dstd.offset) {
			return false;
		}

		if (full && !shortName.equals(dstd.shortName)) {
			return false;
		}

		if (getDst().size() != dstd.getDst().size()) {
			return false;
		}
		if (getDst().isEmpty()) {
			return true;
		}
		if (dstOffset != dstd.dstOffset) {
			return false;
		}
		for (int i = 0; i < getDst().size(); i++) {
			if (getDst().get(i)[0] != dstd.getDst().get(i)[0]) {
				return false;
			}
			if (getDst().get(i)[1] != dstd.getDst().get(i)[1]) {
				return false;
			}
		}

		return true;
	}

	public List<int[]> getDst() {
		return dst;
	}

	public void setDst(List<int[]> dst) {
		this.dst = dst;
	}
}
