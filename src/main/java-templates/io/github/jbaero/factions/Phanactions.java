package io.github.jbaero.factions;

import com.laytonsmith.PureUtilities.SimpleVersion;
import com.laytonsmith.PureUtilities.Version;
import com.laytonsmith.core.CHVersion;
import com.laytonsmith.core.Static;
import com.laytonsmith.core.extensions.AbstractExtension;
import com.laytonsmith.core.extensions.MSExtension;
import com.laytonsmith.core.functions.AbstractFunction;

/**
 * Phanactions, 8/8/2015 10:04 PM
 *
 * @author jb_aero
 */
@MSExtension("${project.name}")
public class Phanactions extends AbstractExtension {

	@Override
	public Version getVersion() {
		return new SimpleVersion("${project.version}");
	}

	@Override
	public void onStartup() {
		Static.getLogger().info("${project.name} ${project.version} loaded.");
	}

	@Override
	public void onShutdown() {
		Static.getLogger().info("${project.name} ${project.version} unloaded.");
	}

}
