package com.sun.electric.tool.generator.infinity;

import java.util.Collection;
import java.util.TreeSet;

import com.sun.electric.tool.generator.layout.LayoutLib;

/** All the channels for one layer */
public class LayerChannels {
	private TreeSet<Channel> channels = new TreeSet<Channel>();
	
	private static void prln(String msg) {System.out.println(msg);}
	private boolean isHorizontal() {
		LayoutLib.error(channels.size()==0, 
				        "can't tell direction because no channels");
		return channels.first().isHorizontal();
	}
	
	public LayerChannels() { }
	public void add(Channel ch) {channels.add(ch);}
	public Collection<Channel> getChannels() {return channels;}
	
	
	public Channel findChanOverVertInterval(double x, double y1, double y2) {
		double yMin = Math.min(y1, y2);
		double yMax = Math.max(y1, y2);
		if (channels.size()==0) return null;
		LayoutLib.error(!isHorizontal(), "not sure what this means yet");
		for (Channel c : channels) {
			LayoutLib.error(x<c.getMinX() || x > c.getMaxX(),
					        "channels can't cover X");
			if (c.getMaxY()<yMin) continue;
			if (c.getMinY()>yMax) break;;
			return c;
		}
		return null;
	}
	
	public Channel findVertBridge(Channel horChan1, Channel horChan2, 
			                      double x1, double x2) {
		if (channels.size()==0) return null;
		double xMin = Math.min(x1, x2);
		double xMax = Math.max(x1, x2);
		LayoutLib.error(isHorizontal(), "channels must be vertical");
		double yMin = Math.min(horChan1.getMinY(), horChan2.getMinY());
		double yMax = Math.max(horChan1.getMaxY(), horChan2.getMaxY());
		for (Channel c : channels) {
			LayoutLib.error(yMax>c.getMaxY() || yMin<c.getMinY(),
					        "channels can't cover Y");
			if (c.getMaxX()<xMin) continue;
			if (c.getMinX()>xMax) break;
			// ideally I should check to see if channel has enough
			// capacity
			return c;
		}
		return null;
	}
	public String toString() {
		StringBuffer sb = new StringBuffer();
//		sb.append(isHorizontal() ? "  Horizontal " : "  Vertical ");
//		sb.append("channel\n");
		for (Channel c : channels)  sb.append(c.toString());
		
		return sb.toString();
	}
	
}
