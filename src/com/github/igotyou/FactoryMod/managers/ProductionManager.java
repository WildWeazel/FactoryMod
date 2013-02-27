package com.github.igotyou.FactoryMod.managers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.inventory.Inventory;

import com.github.igotyou.FactoryMod.FactoryModPlugin;
import com.github.igotyou.FactoryMod.Factorys.Production;
import com.github.igotyou.FactoryMod.interfaces.Factory;
import com.github.igotyou.FactoryMod.interfaces.Manager;
import com.github.igotyou.FactoryMod.properties.ProductionProperties;
import com.github.igotyou.FactoryMod.utility.InteractionResponse;
import com.github.igotyou.FactoryMod.utility.InteractionResponse.InteractionResult;

//original file:
/**
* Manager.java
* Purpose: Interface for Manager objects for basic manager functionality
*
* @author MrTwiggy
* @version 0.1 1/08/13
*/
//edited version:
/**
* Manager.java	 
* Purpose: Interface for Manager objects for basic manager functionality
* @author igotyou
*
*/

public class ProductionManager implements Manager
{
	private FactoryModPlugin plugin;
	private List<Production> producers;
	
	public ProductionManager(FactoryModPlugin plugin)
	{
		this.plugin = plugin;
		producers = new ArrayList<Production>();
		
		updateFactorys();
	}
	
	public void save(File file) throws IOException 
	{
		FileOutputStream fileOutputStream = new FileOutputStream(file);
		BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(fileOutputStream));
		for (Production production : producers)
		{
			//order: subFactoryType world central_x central_y central_z inventory_x inventory_y inventory_z power_x power_y power_z active productionTimer energyTimer current_Recipe_number 
			
			Location centerlocation = production.getCenterLocation();
			Location inventoryLoctation = production.getInventoryLocation();
			Location powerLocation = production.getPowerSourceLocation();
			
			
			
			bufferedWriter.append(production.getSubFactoryType());
			bufferedWriter.append(" ");
			
			bufferedWriter.append(centerlocation.getWorld().getName());
			bufferedWriter.append(" ");
			bufferedWriter.append(Integer.toString(centerlocation.getBlockX()));
			bufferedWriter.append(" ");
			bufferedWriter.append(Integer.toString(centerlocation.getBlockY()));
			bufferedWriter.append(" ");
			bufferedWriter.append(Integer.toString(centerlocation.getBlockZ()));
			bufferedWriter.append(" ");
			
			bufferedWriter.append(Integer.toString(inventoryLoctation.getBlockX()));
			bufferedWriter.append(" ");
			bufferedWriter.append(Integer.toString(inventoryLoctation.getBlockY()));
			bufferedWriter.append(" ");
			bufferedWriter.append(Integer.toString(inventoryLoctation.getBlockZ()));
			bufferedWriter.append(" ");
			
			bufferedWriter.append(Integer.toString(powerLocation.getBlockX()));
			bufferedWriter.append(" ");
			bufferedWriter.append(Integer.toString(powerLocation.getBlockY()));
			bufferedWriter.append(" ");
			bufferedWriter.append(Integer.toString(powerLocation.getBlockZ()));
			bufferedWriter.append(" ");
			
			bufferedWriter.append(Boolean.toString(production.getActive()));
			bufferedWriter.append(" ");
			bufferedWriter.append(Integer.toString(production.getProductionTimer()));
			bufferedWriter.append(" ");
			bufferedWriter.append(Integer.toString(production.getEnergyTimer()));
			bufferedWriter.append(" ");
			bufferedWriter.append(Integer.toString(production.getCurrentRecipeNumber()));
			bufferedWriter.append("\n");
		}
		bufferedWriter.flush();
		fileOutputStream.close();
	}

	public void load(File file) throws IOException 
	{
		FileInputStream fileInputStream = new FileInputStream(file);
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));

		String line;
		while ((line = bufferedReader.readLine()) != null)
		{
			String parts[] = line.split(" ");
			//order: subFactoryType world central_x central_y central_z inventory_x inventory_y inventory_z power_x power_y power_z active productionTimer energyTimer current_Recipe_number 
			String subFactoryType = parts[0];

			Location centerLocation = new Location(plugin.getServer().getWorld(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]), Integer.parseInt(parts[4]));
			Location inventoryLocation = new Location(plugin.getServer().getWorld(parts[1]), Integer.parseInt(parts[5]), Integer.parseInt(parts[6]), Integer.parseInt(parts[7]));
			Location powerLocation = new Location(plugin.getServer().getWorld(parts[1]), Integer.parseInt(parts[8]), Integer.parseInt(parts[9]), Integer.parseInt(parts[10]));
			boolean active = Boolean.parseBoolean(parts[11]);
			int productionTimer = Integer.parseInt(parts[12]);
			int energyTimer = Integer.parseInt(parts[13]);
			int currentRecipeNumber = Integer.parseInt(parts[14]);
			
			Production production = new Production(centerLocation, inventoryLocation, powerLocation, subFactoryType, active, productionTimer, energyTimer, currentRecipeNumber);
			addFactory(production);
		}
		fileInputStream.close();
	}

	public void updateFactorys() 
	{
		plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable()
		{
		    @Override  
		    public void run() 
		    {
		    	for (Production production: producers)
				{
					production.update();
				}
		    }
		}, 0L, FactoryModPlugin.PRODUCER_UPDATE_CYCLE);
	}

	public InteractionResponse createFactory(Location factoryLocation, Location inventoryLocation, Location powerSourceLocation) 
	{
		if (!factoryExistsAt(factoryLocation))
		{
			HashMap<String, ProductionProperties> properties = plugin.production_Properties;
			Block inventoryBlock = inventoryLocation.getBlock();
			Chest chest = (Chest) inventoryBlock.getState();
			Inventory chestInventory = chest.getInventory();
			String subFactoryType = null;
			boolean hasMaterials = true;
			for (Map.Entry<String, ProductionProperties> entry : properties.entrySet())
			{
				HashMap<Integer, Material> buildMaterial = entry.getValue().getBuildMaterial();
				HashMap<Integer, Integer> buildAmount = entry.getValue().getBuildAmount();
				for (int i = 1; i <= buildMaterial.size(); i ++)
				{
					if(!chestInventory.contains(buildMaterial.get(i), buildAmount.get(i)))
					{
						hasMaterials = false;
					}
				}
				if (hasMaterials = true)
				{
					subFactoryType = entry.getKey();
				}
			}
			if (hasMaterials == true && subFactoryType != null)
			{
				Production production = new Production(factoryLocation, inventoryLocation, powerSourceLocation,subFactoryType);
				if (production.buildMaterialAvailable(properties.get(subFactoryType)))
				{
					addFactory(production);
					production.removeBuildMaterial(properties.get(subFactoryType));
					return new InteractionResponse(InteractionResult.SUCCESS, "Successfully created " + subFactoryType + " production factory");
				}
			}
			return new InteractionResponse(InteractionResult.FAILURE, "not enough materials in chest!");
		}
		return new InteractionResponse(InteractionResult.FAILURE, "there is already a factory there!");
	}
	
	public InteractionResponse createFactory(Location factoryLocation, Location inventoryLocation, Location powerSourceLocation, int productionTimer, int energyTimer) 
	{
		if (!factoryExistsAt(factoryLocation))
		{
			HashMap<String, ProductionProperties> properties = plugin.production_Properties;
			Block inventoryBlock = inventoryLocation.getBlock();
			Chest chest = (Chest) inventoryBlock.getState();
			Inventory chestInventory = chest.getInventory();
			String subFactoryType = null;
			boolean hasMaterials = true;
			for (Map.Entry<String, ProductionProperties> entry : properties.entrySet())
			{
				HashMap<Integer, Material> buildMaterial = entry.getValue().getBuildMaterial();
				HashMap<Integer, Integer> buildAmount = entry.getValue().getBuildAmount();
				for (int i = 1; i <= buildMaterial.size(); i ++)
				{
					if(!chestInventory.contains(buildMaterial.get(i), buildAmount.get(i)))
					{
						hasMaterials = false;
					}
				}
				if (hasMaterials = true)
				{
					subFactoryType = entry.getKey();
				}
			}
			if (hasMaterials == true && subFactoryType != null)
			{
				Production production = new Production(factoryLocation, inventoryLocation, powerSourceLocation,subFactoryType);
				if (production.buildMaterialAvailable(properties.get(subFactoryType)))
				{
					addFactory(production);
					production.removeBuildMaterial(properties.get(subFactoryType));
					return new InteractionResponse(InteractionResult.SUCCESS, "Successfully created " + subFactoryType + " production factory");
				}
			}
			return new InteractionResponse(InteractionResult.FAILURE, "not enough materials in chest!");
		}
		return new InteractionResponse(InteractionResult.FAILURE, "there is already a factory there!");
	}

	public InteractionResponse addFactory(Factory factory) 
	{
		Production production = (Production) factory;
		if (production.getCenterLocation().getBlock().getType().equals(Material.WORKBENCH) && (!factoryExistsAt(production.getCenterLocation()))
				|| !factoryExistsAt(production.getInventoryLocation()) || !factoryExistsAt(production.getPowerSourceLocation()))
		{
			producers.add(production);
			return new InteractionResponse(InteractionResult.SUCCESS, "");
		}
		else
		{
			return new InteractionResponse(InteractionResult.FAILURE, "");
		}
	}

	public Production getFactory(Location factoryLocation) 
	{
		for (Production production : producers)
		{
			if (production.getCenterLocation().equals(factoryLocation) || production.getInventoryLocation().equals(factoryLocation)
					|| production.getPowerSourceLocation().equals(factoryLocation))
				return production;
		}
		return null;
	}

	public boolean factoryExistsAt(Location factoryLocation) 
	{
		Location westLocation = factoryLocation.clone();
		Location eastLocation = factoryLocation.clone();
		Location northLocation = factoryLocation.clone();
		Location southLocation = factoryLocation.clone();
		
		westLocation.add(-1,0,0);
		eastLocation.add(1,0,0);
		northLocation.add(0,0,-1);
		southLocation.add(0,0,1);
		
		boolean returnValue = false;
		if (getFactory(factoryLocation) != null)
		{
			returnValue = true;
		}

		if (getFactory(westLocation) != null)
		{
			returnValue = true;
		}
		if (getFactory(eastLocation) != null)
		{
			returnValue = true;
		}
		if (getFactory(northLocation) != null)
		{
			returnValue = true;
		}
		if (getFactory(southLocation) != null)
		{
			returnValue = true;
		}
		return returnValue;
	}

	public void removeFactory(Factory factory) 
	{
		producers.remove((Production)factory);
	}

	public String getSavesFileName() 
	{
		return FactoryModPlugin.PRODUCTION_SAVES_FILE;
	}

}
