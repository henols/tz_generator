package tt;

import java.util.List;

public class DSTDescriptor {

	private ZoneDescriptor zd;

	DSTDescriptor(ZoneDescriptor zd) {
		this.zd = zd;

	}

	public List<int[]> getDst() {
		return zd.getDst();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof DSTDescriptor)) {
			return false;
		}
		DSTDescriptor d = (DSTDescriptor) obj;
		return zd.equals(d.zd, false);
	}

	public ZoneDescriptor getZoneDescriptor() {
		return zd;
	}
}
