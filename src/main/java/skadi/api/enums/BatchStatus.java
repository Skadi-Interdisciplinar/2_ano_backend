package skadi.api.enums;

public enum BatchStatus {
    ACTIVE("ATIVO"),
    BLOCKED("BLOQUEADO"),
    SHIPPED("EXPEDIDO"),
    SOLD("VENDIDO");

    private final String databaseValue;

    BatchStatus(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String getDatabaseValue() {
        return databaseValue;
    }

    public static BatchStatus fromDatabaseValue(String value) {
        for (BatchStatus status : values()) {
            if (status.databaseValue.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Status de lote inválido no banco de dados: " + value);
    }
}
