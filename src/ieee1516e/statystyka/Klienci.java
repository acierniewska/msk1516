package ieee1516e.statystyka;

public class Klienci {
    private int liczbaKlientow;

    private boolean czyNiecierpliwi;
    private boolean pierwszyPosilek = true;

    private boolean czyUsiadl = false;
    private boolean czyDostalPosilek = false;
    private Long czasStaniaWKolejce = -1L;
    private Long czasOczekiwaniaNaPosilek = -1L;
    private Long czasSpozywaniaPosilku = -1L;


    public int getLiczbaKlientow() {
        return liczbaKlientow;
    }

    public void setLiczbaKlientow(int liczbaKlientow) {
        this.liczbaKlientow = liczbaKlientow;
    }

    public boolean isCzyNiecierpliwi() {
        return czyNiecierpliwi;
    }

    public void setCzyNiecierpliwi(boolean czyNiecierpliwi) {
        this.czyNiecierpliwi = czyNiecierpliwi;
    }

    public Long getCzasStaniaWKolejce() {
        return czasStaniaWKolejce;
    }

    public void setCzasStaniaWKolejce(Long czasStaniaWKolejce) {
        this.czasStaniaWKolejce = czasStaniaWKolejce;
    }

    public Long getCzasOczekiwaniaNaPosilek() {
        return czasOczekiwaniaNaPosilek;
    }

    public void setCzasOczekiwaniaNaPosilek(Long czasOczekiwaniaNaPosilek) {
        this.czasOczekiwaniaNaPosilek = czasOczekiwaniaNaPosilek;
    }

    public Long getCzasSpozywaniaPosilku() {
        return czasSpozywaniaPosilku;
    }

    public void setCzasSpozywaniaPosilku(Long czasSpozywaniaPosilku) {
        this.czasSpozywaniaPosilku = czasSpozywaniaPosilku;
    }

    public boolean isPierwszyPosilek() {
        return pierwszyPosilek;
    }

    public void setPierwszyPosilek(boolean pierwszyPosilek) {
        this.pierwszyPosilek = pierwszyPosilek;
    }

    public boolean isCzyUsiadl() {
        return czyUsiadl;
    }

    public void setCzyUsiadl(boolean czyUsiadl) {
        this.czyUsiadl = czyUsiadl;
    }

    public boolean isCzyDostalPosilek() {
        return czyDostalPosilek;
    }

    public void setCzyDostalPosilek(boolean czyDostalPosilek) {
        this.czyDostalPosilek = czyDostalPosilek;
    }
}