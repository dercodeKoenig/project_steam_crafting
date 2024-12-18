package ProjectSteamCrafting.WoodMill;

import java.util.List;

public class WoodMillConfig {

        public float baseResistance;
        public List<MachineRecipe> recipes;

        public float speedMultiplier;

        public static class MachineRecipe {
            public Item inputItem;
            public Item outputItem;
            public float additionalResistance;

            public static class Item {
                public String id;
                public int amount;
            }
        }

}
