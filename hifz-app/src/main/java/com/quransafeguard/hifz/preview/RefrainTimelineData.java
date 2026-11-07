package com.quransafeguard.hifz.preview;

/** Quran/QCF neighbours; visual mnemonic cues are EDITORIAL, never Atlas titles. */
final class RefrainTimelineData {
    private RefrainTimelineData() {}
    // Append-only columns 14..16: stage, prior verse English cue, following verse English cue.
    static final String[][] NODES={
        new String[]{"R047","Al-Qamar","R047-01","1","4","54:17","529","Nūḥ","54:16","Nūḥ : issue du récit","529","54:18","ʿĀd : début du récit","529","Prophetic accounts","Nūḥ's warning","ʿĀd begins"},
        new String[]{"R047","Al-Qamar","R047-02","2","4","54:22","529","ʿĀd","54:21","ʿĀd : issue du récit","529","54:23","Thamūd : début du récit","529","Prophetic accounts","ʿĀd's warning","Thamūd begins"},
        new String[]{"R047","Al-Qamar","R047-03","3","4","54:32","530","Thamūd","54:31","Thamūd : issue du récit","530","54:33","Lūṭ : début du récit","530","Prophetic accounts","Thamūd's fate","Lūṭ begins"},
        new String[]{"R047","Al-Qamar","R047-04","4","4","54:40","530","Lūṭ","54:39","Lūṭ : issue du récit","530","54:41","La famille de Pharaon","530","Prophetic accounts","Lūṭ's warning","Pharaoh's kin"},
        new String[]{"R048","Ar-Raḥmān","R048-01","1","31","55:13","531","Création et ordre","55:12","Grains et plantes aromatiques","531","55:14","Création de l’être humain","531","Creation","Grain, herbs","Clay-born man"},
        new String[]{"R048","Ar-Raḥmān","R048-02","2","31","55:16","531","Création et ordre","55:15","Création des djinns","531","55:17","Deux Orients et deux Occidents","531","Creation","Jinn's fire","Two Easts"},
        new String[]{"R048","Ar-Raḥmān","R048-03","3","31","55:18","531","Création et ordre","55:17","Deux Orients et deux Occidents","531","55:19","Deux mers qui se rencontrent","532","Creation","Two Easts","Two seas"},
        new String[]{"R048","Ar-Raḥmān","R048-04","4","31","55:21","532","Mers et bienfaits","55:20","Barrière entre les deux mers","532","55:22","Perles et corail","532","Sea gifts","Sea barrier","Pearls, coral"},
        new String[]{"R048","Ar-Raḥmān","R048-05","5","31","55:23","532","Mers et bienfaits","55:22","Perles et corail","532","55:24","Navires dans la mer","532","Sea gifts","Pearls, coral","Great ships"},
        new String[]{"R048","Ar-Raḥmān","R048-06","6","31","55:25","532","Mers et bienfaits","55:24","Navires dans la mer","532","55:26","Finitude des créatures","532","Sea gifts","Great ships","All perish"},
        new String[]{"R048","Ar-Raḥmān","R048-07","7","31","55:28","532","Finitude et dépendance","55:27","Permanence du Seigneur","532","55:29","Les créatures Lui demandent","532","Dependence","Lord endures","All petition"},
        new String[]{"R048","Ar-Raḥmān","R048-08","8","31","55:30","532","Finitude et dépendance","55:29","Les créatures Lui demandent","532","55:31","Adresse aux deux communautés","532","Dependence","All petition","Two peoples"},
        new String[]{"R048","Ar-Raḥmān","R048-09","9","31","55:32","532","Interpellation et jugement","55:31","Adresse aux deux communautés","532","55:33","Limites des humains et des djinns","532","Reckoning","Two peoples","No escape"},
        new String[]{"R048","Ar-Raḥmān","R048-10","10","31","55:34","532","Interpellation et jugement","55:33","Limites des humains et des djinns","532","55:35","Flammes et métal fondu","532","Reckoning","No escape","Flames, copper"},
        new String[]{"R048","Ar-Raḥmān","R048-11","11","31","55:36","532","Interpellation et jugement","55:35","Flammes et métal fondu","532","55:37","Ciel déchiré","532","Reckoning","Flames, copper","Sky splits"},
        new String[]{"R048","Ar-Raḥmān","R048-12","12","31","55:38","532","Interpellation et jugement","55:37","Ciel déchiré","532","55:39","Humains, djinns et leurs fautes","532","Reckoning","Sky splits","No questioning"},
        new String[]{"R048","Ar-Raḥmān","R048-13","13","31","55:40","532","Interpellation et jugement","55:39","Humains, djinns et leurs fautes","532","55:41","Les criminels reconnus","532","Reckoning","No questioning","Sinners marked"},
        new String[]{"R048","Ar-Raḥmān","R048-14","14","31","55:42","533","Interpellation et jugement","55:41","Les criminels reconnus","532","55:43","La Géhenne déniée","533","Reckoning","Sinners marked","Hellfire"},
        new String[]{"R048","Ar-Raḥmān","R048-15","15","31","55:45","533","Interpellation et jugement","55:44","Entre Géhenne et eau brûlante","533","55:46","Deux jardins pour qui craint","533","Reckoning","Boiling water","Two gardens"},
        new String[]{"R048","Ar-Raḥmān","R048-16","16","31","55:47","533","Deux jardins","55:46","Deux jardins pour qui craint","533","55:48","Des branches","533","First gardens","Two gardens","Branches"},
        new String[]{"R048","Ar-Raḥmān","R048-17","17","31","55:49","533","Deux jardins","55:48","Des branches","533","55:50","Deux sources qui coulent","533","First gardens","Branches","Flowing springs"},
        new String[]{"R048","Ar-Raḥmān","R048-18","18","31","55:51","533","Deux jardins","55:50","Deux sources qui coulent","533","55:52","Fruits par paires","533","First gardens","Flowing springs","Paired fruits"},
        new String[]{"R048","Ar-Raḥmān","R048-19","19","31","55:53","533","Deux jardins","55:52","Fruits par paires","533","55:54","Couches et fruits à portée","533","First gardens","Paired fruits","Silken couches"},
        new String[]{"R048","Ar-Raḥmān","R048-20","20","31","55:55","533","Deux jardins","55:54","Couches et fruits à portée","533","55:56","Regards réservés","533","First gardens","Silken couches","Modest gaze"},
        new String[]{"R048","Ar-Raḥmān","R048-21","21","31","55:57","533","Deux jardins","55:56","Regards réservés","533","55:58","Rubis et corail","533","First gardens","Modest gaze","Rubies, coral"},
        new String[]{"R048","Ar-Raḥmān","R048-22","22","31","55:59","533","Deux jardins","55:58","Rubis et corail","533","55:60","Récompense de la bienfaisance","533","First gardens","Rubies, coral","Good repaid"},
        new String[]{"R048","Ar-Raḥmān","R048-23","23","31","55:61","533","Deux jardins","55:60","Récompense de la bienfaisance","533","55:62","Deux autres jardins","533","First gardens","Good repaid","Other gardens"},
        new String[]{"R048","Ar-Raḥmān","R048-24","24","31","55:63","533","Deux autres jardins","55:62","Deux autres jardins","533","55:64","Verdure profonde","533","Other gardens","Other gardens","Deep green"},
        new String[]{"R048","Ar-Raḥmān","R048-25","25","31","55:65","533","Deux autres jardins","55:64","Verdure profonde","533","55:66","Deux sources jaillissantes","533","Other gardens","Deep green","Gushing springs"},
        new String[]{"R048","Ar-Raḥmān","R048-26","26","31","55:67","533","Deux autres jardins","55:66","Deux sources jaillissantes","533","55:68","Fruits, palmiers et grenades","533","Other gardens","Gushing springs","Dates, pomegranates"},
        new String[]{"R048","Ar-Raḥmān","R048-27","27","31","55:69","533","Deux autres jardins","55:68","Fruits, palmiers et grenades","533","55:70","Compagnes bonnes et belles","534","Other gardens","Dates, pomegranates","Beautiful companions"},
        new String[]{"R048","Ar-Raḥmān","R048-28","28","31","55:71","534","Deux autres jardins","55:70","Compagnes bonnes et belles","534","55:72","Compagnes dans les pavillons","534","Other gardens","Beautiful companions","Pavilions"},
        new String[]{"R048","Ar-Raḥmān","R048-29","29","31","55:73","534","Deux autres jardins","55:72","Compagnes dans les pavillons","534","55:74","Compagnes préservées","534","Other gardens","Pavilions","Untouched companions"},
        new String[]{"R048","Ar-Raḥmān","R048-30","30","31","55:75","534","Deux autres jardins","55:74","Compagnes préservées","534","55:76","Coussins verts et beaux tapis","534","Other gardens","Untouched companions","Green cushions"},
        new String[]{"R048","Ar-Raḥmān","R048-31","31","31","55:77","534","Deux autres jardins","55:76","Coussins verts et beaux tapis","534","55:78","Clôture : majesté et générosité","534","Other gardens","Green cushions","Majestic Lord"},
        new String[]{"R059","Al-Mursalāt","R059-01","1","10","77:15","580","Décision, peuples et création","77:14","Le Jour de la Décision","580","77:16","Destruction des premiers peuples","580","Decision","Decision Day","Ancient peoples"},
        new String[]{"R059","Al-Mursalāt","R059-02","2","10","77:19","580","Décision, peuples et création","77:18","Sort des criminels","580","77:20","Création humaine","581","Creation","Sinners' fate","Human creation"},
        new String[]{"R059","Al-Mursalāt","R059-03","3","10","77:24","581","Décision, peuples et création","77:23","Mesure de la création","581","77:25","La terre comme réceptacle","581","Creation","Creation measured","Earth's embrace"},
        new String[]{"R059","Al-Mursalāt","R059-04","4","10","77:28","581","Décision, peuples et création","77:27","Montagnes et eau douce","581","77:29","Départ vers le châtiment dénié","581","Creation","Mountains, water","Punishment ahead"},
        new String[]{"R059","Al-Mursalāt","R059-05","5","10","77:34","581","Scènes du jugement","77:33","Étincelles du brasier","581","77:35","Jour sans parole","581","Judgement","Blazing sparks","Silent day"},
        new String[]{"R059","Al-Mursalāt","R059-06","6","10","77:37","581","Scènes du jugement","77:36","Aucune excuse permise","581","77:38","Rassemblement au Jour de la Décision","581","Judgement","No excuses","Gathering"},
        new String[]{"R059","Al-Mursalāt","R059-07","7","10","77:40","581","Scènes du jugement","77:39","Défi aux stratagèmes","581","77:41","Ombres et sources des pieux","581","Judgement","Vain schemes","Shade, springs"},
        new String[]{"R059","Al-Mursalāt","R059-08","8","10","77:45","581","Séjour des pieux","77:44","Rétribution des bienfaisants","581","77:46","Jouissance brève des criminels","581","Reward","Good rewarded","Brief enjoyment"},
        new String[]{"R059","Al-Mursalāt","R059-09","9","10","77:47","581","Derniers avertissements","77:46","Jouissance brève des criminels","581","77:48","Refus de s’incliner","581","Final warning","Brief enjoyment","Refused bowing"},
        new String[]{"R059","Al-Mursalāt","R059-10","10","10","77:49","581","Derniers avertissements","77:48","Refus de s’incliner","581","77:50","Conclusion : quelle parole croiront-ils ?","581","Final warning","Refused bowing","Which message?"}
    };
}
