# Glossary

**Terms, Expressions and Acronyms (TEA)**

| **_TEA_** (EN)           | **_Description_** (EN)                                                                                                                                       |
|:-------------------------|:-------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Activity Level**       | An evaluation of a user's daily physical activity and lifestyle intensity, used as a baseline parameter to calculate accurate nutritional and calorie goals. |
| **Barcode**              | A unique scannable optical code associated with a food item for quick identification and lookup in the catalog.                                              |
| **Brand** | The brand or manufacturer name associated with a food item. |
| **Calorie Target**       | The target amount of calories a user aims to consume daily, whether computed automatically from their profile or overridden manually.                        |
| **Custom Recipe**        | A user-created recipe composed of multiple recipe ingredients, allowing users to log homemade meals and dishes not found in the public catalog.              |
| **Daily Goal Snapshot** | The calorie and macronutrient targets retained by a daily log for its date, so progress and history use the targets associated with that day. |
| **Daily Log**            | The aggregate representing a single day of tracking for a user, containing all recorded meals and food consumption items for that date.                      |
| **Daily Log Id**         | The unique identifier of a daily log record.                                                                                                                 |
| **Daily Nutrition Summary** | The calculated calories, protein, carbohydrates, and fat consumed during a day, together with daily targets and remaining amounts. Negative remaining amounts indicate an exceeded target. |
| **Email**                | A unique valid email address used for user identification, authentication, and communication within the application.                                         |
| **Food Catalog**         | The aggregated collection of available food items that users can search through to select and log items.                                                     |
| **Food Id**              | The unique identifier assigned to a specific food item in the catalog.                                                                                       |
| **Food Item**            | A commercially or generically available food product in the catalog, characterized by its name, barcode, nutrient composition, and serving size.             |
| **Food Name**            | The official designated name or commercial title of a food item.                                                                                             |
| **Food Snapshot** | The food details captured when a meal item is logged, including its name, optional brand/barcode, nutrient values and basis, and serving definition. These values belong to the daily log and preserve the recorded intake independently of later source changes. |
| **Ingredient Quantity** | The positive amount and unit of an ingredient used in a recipe, such as servings, grams, or milliliters. It determines how the ingredient's nutrient values contribute to the recipe totals. |
| **Ingredient Snapshot** | The food details captured for a recipe ingredient, including its name, optional brand/barcode, nutrient values and basis, and serving definition. These values belong to the recipe and remain independent of later source changes. |
| **Log Date**             | The specific calendar date associated with a daily log record.                                                                                               |
| **Macro Breakdown**      | The distribution and target gram amounts for major macronutrients (proteins, carbohydrates, and fats) required to meet a user's nutritional goal.            |
| **Meal**                 | A specific eating occasion within a daily log (such as breakfast, lunch, or dinner) composed of multiple meal items.                                         |
| **Meal Item**            | An individual entry within a meal, specifying a portion quantity of a food item or a custom recipe.                                                          |
| **Meal Type**            | The category or classification of a meal, such as breakfast, lunch, dinner, or snack.                                                                        |
| **Nutrient Composition** | The detailed breakdown of nutritional values (such as calories, proteins, fats, carbohydrates, and vitamins) per serving size of a food item.                |
| **Nutritional Goal**     | The set of dietary objectives defined for a user, comprising a calorie target and a macro breakdown.                                                         |
| **Password**             | The confidential secret credential used to authenticate a user securely.                                                                                     |
| **Physical Data**        | The physical attributes of a user (such as age, weight, height, and gender) used to model nutritional needs and calculate goals.                             |
| **Portion Quantity**     | The specific amount or number of servings consumed for a food item or custom recipe within a meal item.                                                      |
| **Product Name** | The name identifying a food product in the catalog; represented as ProductName in the domain model. See also Food Name. |
| **Progress Tracker**     | An aggregate that monitors a user's long-term journey, compiling weight records and trend analyses to evaluate the effectiveness of their nutrition plan.    |
| **Recipe Id**            | The unique identifier assigned to a custom recipe.                                                                                                           |
| **Recipe Ingredients** | The ingredient entries owned by a custom recipe. Each RecipeIngredients entity in the domain model represents one identified row containing an Ingredient Snapshot and Ingredient Quantity. |
| **Recipe Macros** | The entity owned by a custom recipe that persists whole-recipe calories and grams of protein, carbohydrates, and fat, calculated by summing scaled ingredient contributions. Per-serving values are these totals divided by the recipe's numberOfServings attribute. |
| **Recipe Name**          | The title or designated name of a custom recipe.                                                                                                             |
| **Recipe Yield**         | The positive number of servings produced by the complete recipe, stored as the numberOfServings attribute of CustomRecipe rather than a separate value object. Calories and macros per serving equal the whole-recipe totals divided by this yield. For example, a 600 kcal recipe yielding 3 servings provides 200 kcal per serving. |
| **Serving Size**         | The standard reference quantity (e.g., 100g, 1 cup, 1 piece) used to measure a food item's nutrient composition.                                             |
| **Trend Analysis**       | Analytical insights derived from past progress data, showcasing weight and nutrition trends over time.                                                       |
| **User**                 | The entity root representing a system user, managing authentication, profile data, and nutritional objectives.                                               |
| **User Id**              | The unique identifier assigned to a user entity.                                                                                                             |
| **User Nutritional Profile** | The user-owned aggregate containing physical data, activity level, and nutritional goals. Represented as UserNutritionalProfile in the domain model; see also User Profile. |
| **User Profile**         | The internal profile structure of a user, encapsulating their physical data and activity level.                                                              |
| **Weight Record**        | A historical data point recording a user's body weight at a specific date, used for progress tracking and trend analysis.                                    |
