package se.aceone.tz_gen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;


public class TreeBuilder {
	HashMap<String, ZoneDescriptor> zds = new HashMap<String, ZoneDescriptor>();
	HashMap<String, TreeBuilder> treeNodes = new HashMap<String, TreeBuilder>();
	TreeBuilder parent = null;
	private String name;

	public TreeBuilder() {
	}

	public TreeBuilder(String name, TreeBuilder parent) {
		this.name = name;
		this.parent = parent;
	}

	void add(String name, ZoneDescriptor zd) {
		int ind = name.indexOf('/');
		if (ind < 0) {
			zds.put(name, zd);
		} else {
			String key = name.substring(0, ind);
			TreeBuilder node = treeNodes.get(key);
			if (node == null) {
				node = new TreeBuilder(key, this);
				treeNodes.put(key, node);
			}
			node.add(name.substring(ind + 1), zd);
		}
	}

	@Override
	public String toString() {
		return toString("");
	}

	public String getMethodName() {
		if (parent == null) {
			return "lookup_tz";
		}
		return parent.getMethodName() + "_" + name;
	}

	public String toString(String ind) {
		String s = "";
		for (String key : zds.keySet()) {
			s += ind + "-- " + key + "\n";
		}
		for (String key : treeNodes.keySet()) {
			s += ind + key + "\n";
			s += treeNodes.get(key).toString(ind + "  ");
		}
		return s;
	}

	public List<TreeBuilder> getAllChildren() {
		List<TreeBuilder> c = new ArrayList<TreeBuilder>();
		// TODO Auto-generated method stub
		getAllChildren(c);

		return c;
	}

	private void getAllChildren(List<TreeBuilder> cList) {
		for (TreeBuilder child : treeNodes.values()) {
			cList.add(child);
			child.getAllChildren(cList);
		}
	}

	public Collection<TreeBuilder> getTreeBuilderChildren() {
		return treeNodes.values();
	}

	public String getName() {
		return name;
	}

	public Collection<ZoneDescriptor> getZoneDescChildren() {
		return zds.values();
	}
}
