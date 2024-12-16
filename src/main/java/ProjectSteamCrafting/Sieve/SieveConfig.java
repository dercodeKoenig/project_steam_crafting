package ProjectSteamCrafting.Sieve;

import java.util.List;

public class SieveConfig {

        public float baseResistance;
        public float k;
        public float clickForce;
        public List<MachineRecipe> recipes;

        public static class MachineRecipe {
            public List<Item> inputItems;
            public List<Item> outputItems;
            public float timeRequired;
            public float additionalResistance;

            public float currentProgress = 0;

            public static class Item {
                public String id;
                public int amount;
                public float p;
            }
        }

}
