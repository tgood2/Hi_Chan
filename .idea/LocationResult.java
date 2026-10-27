Public class LocationResult {
    private String name;
    private String direction;
    private String channel;

    public LocationResult(String name, String direction, String channel) {
        this.name = name;
        this.direction = direction;
        this.channel = channel;
    }

    public String getName() {
        return name;
    }

    public String getDirection() {
        return direction;
    }

    public String getChannel() {
        return channel;
    }
}