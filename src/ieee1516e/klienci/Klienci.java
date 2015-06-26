package ieee1516e.klienci;

public class Klienci {
    private int liczbaKlientow;
    private int idStolika;

    private boolean czyNiecierpliwi;
    private boolean pierwszyPosilek = true;

    private short czasStaniaWKolejce;
    private short czasOczekiwaniaNaPosilek = -1;
    private short czasSpozywaniaPosilku = -1;


    public int getLiczbaKlientow() {
        return liczbaKlientow;
    }

    public void setLiczbaKlientow(int liczbaKlientow) {
        this.liczbaKlientow = liczbaKlientow;
    }

    public int getIdStolika() {
        return idStolika;
    }

    public void setIdStolika(int idStolika) {
        this.idStolika = idStolika;
    }

    public boolean isCzyNiecierpliwi() {
        return czyNiecierpliwi;
    }

    public void setCzyNiecierpliwi(boolean czyNiecierpliwi) {
        this.czyNiecierpliwi = czyNiecierpliwi;
    }

    public short getCzasStaniaWKolejce() {
        return czasStaniaWKolejce;
    }

    public void setCzasStaniaWKolejce(short czasStaniaWKolejce) {
        this.czasStaniaWKolejce = czasStaniaWKolejce;
    }

    public short getCzasOczekiwaniaNaPosilek() {
        return czasOczekiwaniaNaPosilek;
    }

    public void setCzasOczekiwaniaNaPosilek(short czasOczekiwaniaNaPosilek) {
        this.czasOczekiwaniaNaPosilek = czasOczekiwaniaNaPosilek;
    }

    public short getCzasSpozywaniaPosilku() {
        return czasSpozywaniaPosilku;
    }

    public void setCzasSpozywaniaPosilku(short czasSpozywaniaPosilku) {
        this.czasSpozywaniaPosilku = czasSpozywaniaPosilku;
    }

    public boolean isPierwszyPosilek() {
        return pierwszyPosilek;
    }

    public void setPierwszyPosilek(boolean pierwszyPosilek) {
        this.pierwszyPosilek = pierwszyPosilek;
    }
}