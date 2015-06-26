package ieee1516e.menedzerSali;

public class Stolik {
    private int idKlienta;
    private int liczbaMiejsc;
    private boolean zajety;


    public Stolik(int liczbaMiejsc, boolean zajety) {
        this.liczbaMiejsc = liczbaMiejsc;
        this.zajety = zajety;
    }

    public int getLiczbaMiejsc() {
        return liczbaMiejsc;
    }

    public void setLiczbaMiejsc(int liczbaMiejsc) {
        this.liczbaMiejsc = liczbaMiejsc;
    }

    public boolean isZajety() {
        return zajety;
    }

    public void setZajety(boolean zajety) {
        this.zajety = zajety;
    }

    public int getIdKlienta() {
        return idKlienta;
    }

    public void setIdKlienta(int idKlienta) {
        this.idKlienta = idKlienta;
    }
}
