package ieee1516e.klienci;

public class DostarczenieZamowieniaEvent {
    private int stolikId;
    private String listaPosilkow;
    private long czas;

    public DostarczenieZamowieniaEvent(int stolikId, String listaPosilkow, long czas) {
        this.stolikId = stolikId;
        this.listaPosilkow = listaPosilkow;
        this.czas = czas;
    }

    public int getStolikId() {
        return stolikId;
    }

    public void setStolikId(int stolikId) {
        this.stolikId = stolikId;
    }

    public String getListaPosilkow() {
        return listaPosilkow;
    }

    public void setListaPosilkow(String listaPosilkow) {
        this.listaPosilkow = listaPosilkow;
    }

    public long getCzas() {
        return czas;
    }

    public void setCzas(long czas) {
        this.czas = czas;
    }
}
