package ieee1516e.klienci;

public class InterakcjaZMenedzeremEvent {
    private int idKlientow;
    private int idStolika;
    private long czas;

    public InterakcjaZMenedzeremEvent(int idKlientow, int idStolika, long czas) {
        this.idKlientow = idKlientow;
        this.idStolika = idStolika;
        this.czas = czas;
    }

    public int getIdKlientow() {
        return idKlientow;
    }

    public void setIdKlientow(int idKlientow) {
        this.idKlientow = idKlientow;
    }

    public int getIdStolika() {
        return idStolika;
    }

    public void setIdStolika(int idStolika) {
        this.idStolika = idStolika;
    }

    public long getCzas() {
        return czas;
    }

    public void setCzas(long czas) {
        this.czas = czas;
    }
}
