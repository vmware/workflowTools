public class PlaceLocation {
    public String name;
    public String address;

    public boolean isNotInUsa() {
        return address != null && !address.endsWith("USA") && !address.endsWith("United States");
    }

    public String country() {
        if (address == null) {
            return null;
        }
        String countryMatch = address.substring(address.lastIndexOf("\n") + 1);
        String trimmedCountry;
        if (!countryMatch.contains(",")) {
            trimmedCountry = countryMatch.trim();
        } else  {
            trimmedCountry = countryMatch.substring(countryMatch.lastIndexOf(",") + 1).trim();
        }

        switch (trimmedCountry) {
        case "United States":
            return "USA";
        case "Italia":
            return "Italy";
        case "Tanzánie":
            return "Tanzania";
        case "Éire":
            return "Ireland";
        case "United Kingdom":
            return "UK";
        default:
            return trimmedCountry;
        }
    }


}
