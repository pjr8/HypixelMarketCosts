package me.pjr8;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.nullicorn.nedit.NBTReader;
import me.nullicorn.nedit.type.NBTCompound;
import net.hypixel.api.HypixelAPI;
import net.hypixel.api.reply.skyblock.BazaarReply;
import net.hypixel.api.reply.skyblock.SkyBlockAuctionsReply;

import java.io.*;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

public class Main {

    public static UUID API_KEY = UUID.fromString("API HERE FROM DEV SITE");
    public static HypixelAPI api = new HypixelAPI(API_KEY);
    public static BazaarReply bazaar;
    public static ArrayList<SkyBlockAuctionsReply> auctions = new ArrayList<>();
    public static String path = "link to NEU repo's items folder";
    public static JsonParser parser = new JsonParser();
    public static DecimalFormat format = new DecimalFormat("#,###.00");

    public static HashMap<String, ArrayList<Integer>> prices = new HashMap<>();

    public static HashMap<String, Double> bazaarInstantBuyPrices = new HashMap<>();
    public static HashMap<String, Double> bazaarBuyOrderPrices = new HashMap<>();



    public static void main(String[] args) {
        try {
            bazaar = api.getBazaar().get();
            auctions.add(api.getSkyBlockAuctions(0).get());
            int i = 0;
            while (auctions.get(i).hasNextPage()) {
                auctions.add(api.getSkyBlockAuctions(i + 1).get());
                i++;
            }
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        setAuctionPrices();
        setBazaarPrices();

        //List<String> toCheck = List.of("TITANIUM_DRILL_1", "TITANIUM_DRILL_2", "TITANIUM_DRILL_3", "TITANIUM_DRILL_4");
        //List<String> toCheck = List.of("TERMINATOR");

        //checkBazaarCraftup();
        //calculateBazaar(toCheck);
        //bazaarDiscrepancies();




        auctions.forEach(auction -> {
            JsonArray jsonArray = auction.getAuctions().getAsJsonArray();
            jsonArray.forEach(jsonElement -> {
                JsonObject object = jsonElement.getAsJsonObject();
                JsonElement bin = object.get("bin");
                if (bin != null && bin.getAsBoolean()) {
                    String id = getItemIDFromAuction(object);
                    if (Objects.equals(id, "TERMINATOR")) {
                        try {
                            NBTCompound nbtCompound = NBTReader.readBase64(object.get("item_bytes").getAsString());
                            String toPrint = nbtCompound.getList("i").getCompound(0).getCompound("tag").
                                    getCompound("ExtraAttributes").toString();
                            System.out.println(toPrint);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            });
        });

        System.out.println(bazaarInstantBuyPrices.get("THE_ART_OF_WAR"));
        System.out.println(bazaarBuyOrderPrices.get("THE_ART_OF_WAR"));


        System.out.println("Done!");
    }


    public static void setBazaarPrices() {
        bazaar.getProducts().forEach((item, product) -> {
            if (!product.getBuySummary().isEmpty() && !product.getSellSummary().isEmpty()) {
                bazaarInstantBuyPrices.put(item, product.getSellSummary().get(0).getPricePerUnit());
                bazaarBuyOrderPrices.put(item, product.getBuySummary().get(0).getPricePerUnit());
            }
        });
    }



    public static void setAuctionPrices() {
        auctions.forEach(auction -> {
            JsonArray jsonArray = auction.getAuctions().getAsJsonArray();
            jsonArray.forEach(jsonElement -> {
                JsonObject object = jsonElement.getAsJsonObject();
                JsonElement bin = object.get("bin");
                if (bin != null && bin.getAsBoolean()) {
                    String id = getItemIDFromAuction(object);
                    if (id != null) {
                        if (prices.containsKey(id)) {
                            prices.get(id).add(object.get("starting_bid").getAsInt());
                        } else {
                            ArrayList<Integer> list = new ArrayList<>();
                            list.add(object.get("starting_bid").getAsInt());
                            prices.put(id, list);
                        }
                    }
                }
            });
        });
        prices.forEach((id, list) -> list.sort(Comparator.comparingDouble(Integer::doubleValue)));
    }

    public static void checkBazaarCraftup() {
        HashMap<String, Double> profits = new HashMap<>();
        bazaar.getProducts().forEach((item, product) -> {
            if (!item.contains("ENCHANTMENT")) {
                profits.put(item, getCostBuyOrder(getRequirements(item)));
            }
        });
        profits.entrySet().stream().sorted(Map.Entry.comparingByValue()).forEach(entry -> System.out.println("Item: " + entry.getKey() + " | Cost: " + format.format(entry.getValue())));

    }


    public static void bazaarDiscrepancies() {
        bazaar.getProducts().forEach((key, value) -> {
            double buyOrderPrice = value.getQuickStatus().getBuyPrice();
            double instaBuyPrice = value.getQuickStatus().getSellPrice();


            double priceToCheck = instaBuyPrice * 1.5;
            if (buyOrderPrice > priceToCheck && (buyOrderPrice - priceToCheck) > 100) {
                if (!key.contains("ENCHANTMENT") && value.getQuickStatus().getBuyMovingWeek() > 500_000 && value.getQuickStatus().getSellMovingWeek() > 1_000_000) {
                    double profitPer = (buyOrderPrice - priceToCheck) * 0.985;
                    System.out.println("Profit: " + format.format(profitPer) + " | Item: " + key + " | Buy Price: " + format.format(value.getQuickStatus().getBuyPrice()) + " | Sell Price: " +
                            format.format(value.getQuickStatus().getSellPrice()) + " | Volume: " + format.format(value.getQuickStatus().getBuyMovingWeek()) + " :  " + format.format(value.getQuickStatus().getSellMovingWeek()));
                }
            }
        });
    }


    public static void calculateBazaar(List<String> toCheck) {

        toCheck.forEach(item -> {
            Map<String, Integer> requirements = getRequirements(item);
            System.out.println("Buy Order buy ======================================");
            System.out.println("End Item: " + item + " | Buy Order Cost: " + format.format(getCostBuyOrder(requirements)));
            System.out.println("Insta buy ======================================");
            System.out.println("End Item: " + item + " | Insta-Buy Cost: " + format.format(getCostInstaBuy(requirements)));
        });
    }



    public static String getItemIDFromAuction(JsonObject jsonObject) {
        String toReturn = null;
        try {
            NBTCompound nbtCompound = NBTReader.readBase64(jsonObject.get("item_bytes").getAsString());
            toReturn = nbtCompound.getList("i").getCompound(0).getCompound("tag").
                    getCompound("ExtraAttributes").getString("id");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return toReturn;
    }

    public static double getCostInstaBuy(Map<String, Integer> requirements) {
        AtomicReference<Double> cost = new AtomicReference<>((double) 0);
        requirements.forEach((item, amount) -> {
            BazaarReply.Product product = bazaar.getProduct(item);
            if (product != null && !product.getBuySummary().isEmpty()) {
                cost.updateAndGet(v -> v + product.getBuySummary().get(0).getPricePerUnit() * amount);
                //System.out.println("Item: " + item + " | Amount: " + amount + " | Cost: " + format.format(product.getBuySummary().get(0).getPricePerUnit() * amount));
            } else {
                if (prices.containsKey(item)) {
                    cost.updateAndGet(v -> v + prices.get(item).get(0) * amount);
                    //System.out.println("Item: " + item + " | Amount: " + amount + " | Cost: " + format.format((long) prices.get(item).get(0) * amount));
                }
            }
        });
        return cost.get();
    }

    public static double getCostBuyOrder(Map<String, Integer> requirements) {
        AtomicReference<Double> cost = new AtomicReference<>((double) 0);
        requirements.forEach((item, amount) -> {
            BazaarReply.Product product = bazaar.getProduct(item);
            if (product != null && !product.getSellSummary().isEmpty()) {
                cost.updateAndGet(v -> v + product.getSellSummary().get(0).getPricePerUnit() * amount);
                System.out.println("Item: " + item + " | Amount: " + amount + " | Cost: " + format.format(product.getSellSummary().get(0).getPricePerUnit() * amount));
            } else {
                if (prices.containsKey(item)) {
                    cost.updateAndGet(v -> v + prices.get(item).get(0) * amount);
                    System.out.println("Item: " + item + " | Amount: " + amount + " | Cost: " + format.format((long) prices.get(item).get(0) * amount));
                }
            }
        });
        return cost.get();
    }


    private static Map<String, Integer> getRequirements(String masterItem) {
        ConcurrentHashMap<String, Integer> toReturn = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, Integer> itemsToProcess = new ConcurrentHashMap<>();
        Objects.requireNonNull(loadRecipe(masterItem)).forEach((key, value) -> {
            if (isLowest(key)) {
                addToMapLoadRecipe(key, value, toReturn);
            } else {
                itemsToProcess.put(key, value);
            }
        });
        boolean finished = false;

        while (!finished) {
            if (itemsToProcess.isEmpty()) {
                finished = true;
                continue;
            }
            itemsToProcess.forEach((key, value) -> {
                ConcurrentHashMap<String, Integer> recipe = loadRecipe(key);
                if (recipe == null) {
                    addToMapLoadRecipe(key, value, toReturn);
                    itemsToProcess.remove(key);
                    return;
                }
                if (!recipe.isEmpty()) {
                    List<String> keysToRemove = new ArrayList<>();
                    recipe.forEach((k, v) -> {
                        if (isLowest(k)) {
                            addToMapLoadRecipe(k, v * value, toReturn);
                            keysToRemove.add(k);
                        }
                    });
                    keysToRemove.forEach(recipe::remove);
                }
                if (!recipe.isEmpty()) {
                    recipe.forEach((k, v) -> {
                        if (itemsToProcess.containsKey(k)) {
                            itemsToProcess.put(k, itemsToProcess.get(k) + v * value);
                        } else {
                            itemsToProcess.put(k, v * value);
                        }
                    });
                }
                itemsToProcess.remove(key);
            });
        }
        return toReturn;
    }

    public static ConcurrentHashMap<String, Integer> loadRecipe(String name) {
        Map<String, Integer> toReturn = new HashMap<>();
        //System.out.println("Loading Recipe: " + name);
        try {
            File file = new File(path + name + ".json");
            if (file.exists()) {
                JsonObject object = parser.parse(Files.readString(file.toPath())).getAsJsonObject();
                if (object.has("recipe")) {
                    object = object.getAsJsonObject("recipe");
                    object.entrySet().forEach(entry -> {
                        if (!entry.getValue().getAsString().isEmpty() && !entry.getKey().equals("count")) {
                            String[] split = entry.getValue().getAsString().split(":");
                            addToMapLoadRecipe(split[0], Integer.parseInt(split[1]), toReturn);
                        }
                    });
                } else if (object.has("recipes")) {
                    JsonArray recipes = object.getAsJsonArray("recipes");
                    JsonArray items = recipes.get(0).getAsJsonObject().getAsJsonArray("inputs");
                    items.forEach(item -> {
                        String[] split = item.getAsString().split(":");
                        addToMapLoadRecipe(split[0], Integer.parseInt(split[1]), toReturn);
                    });
                } else {
                    return null;
                }
            } else {
                System.out.println("File not found: " + name);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //System.out.println("Loaded Recipe: " + name + " | Recipe: " + toReturn);
        return new ConcurrentHashMap<>(toReturn);
    }

    public static void addToMapLoadRecipe(String key, int amount, Map<String, Integer> map) {
        if (map.containsKey(key)) {
            map.put(key, map.get(key) + amount);
        } else {
            map.put(key, amount);
        }
    }

    private static final List<String> lowest = List.of("IRON_INGOT", "GOLD_INGOT", "DIAMOND", "COAL", "REDSTONE", "INK_SACK-4");

    public static boolean isLowest(String item) {
        for (String s : lowest) {
            if (s.equals(item)) {
                return true;
            }
        }
        return false;
    }
}