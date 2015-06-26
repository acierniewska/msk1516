package ieee1516e.kelner;

public class ZamowienieEvent {
    private int stolikId;
    private String listaPosilkow;

    public ZamowienieEvent(int stolikId, String listaPosilkow) {
        this.stolikId = stolikId;
        this.listaPosilkow = listaPosilkow;
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
}
