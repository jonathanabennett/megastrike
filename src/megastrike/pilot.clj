(ns megastrike.pilot
  "Handles pilots and crews for units.
  
  `display` prints the pilot's stats as name(skill), so 'Bob Kim(4)'
  Eventually, this will handle Special Pilot Abilities, which is why I didn't delete it
  when removing records.")

(defn display
  "Formats the pilot information in the following format: 'Name(skill)'
  Examples:
  Bob Kim(4)
  Shooty McShootyface (2)"
  [{:keys [pilot/full-name pilot/skill]}]
  (str full-name " (" skill ")"))
