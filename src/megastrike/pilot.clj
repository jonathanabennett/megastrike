(ns megastrike.pilot
  "Handles pilots and crews for units.
  
  `display` prints the pilot's stats as name(skill), so 'Bob Kim(4)'
  Eventually, this will handle Special Pilot Abilities, which is why I didn't delete it
  when removing records.")

(defn display
  "Formats the pilot information in the following format: 'Name(skill)'
  Precondition: A valid pilot map containing the keys `pilot/full-name` and `pilot/skill`
  Postcondition: A string is returned containing the pilot's full name and skill per these examples.
  Examples:
  Bob Kim(4)
  Shooty McShootyface (2)
 
  @param pilot-map: A map containing pilot information.
  @return: A string matching the examples above."
  [{:keys [pilot/full-name pilot/skill]}]
  (str full-name " (" skill ")"))
