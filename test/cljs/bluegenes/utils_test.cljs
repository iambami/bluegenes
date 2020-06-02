(ns bluegenes.utils-test
  (:require [cljs.test :refer-macros [deftest is are testing]]
            [bluegenes.utils :as utils]))

(deftest read-xml-query
  (testing "Missing fields are correctly nulled"
    (are [xml m] (= (utils/read-xml-query xml) m)
      "<query name=\"\" model=\"genomic\" view=\"Gene.secondaryIdentifier Gene.symbol Gene.organism.name Gene.primaryIdentifier\" longDescription=\"\"></query>"
      {:from "Gene",
       :select
       ["Gene.secondaryIdentifier"
        "Gene.symbol"
        "Gene.organism.name"
        "Gene.primaryIdentifier"],
       :orderBy [],
       :constraintLogic nil,
       :joins [],
       :where []}
      "<query name=\"\" model=\"genomic\" view=\"Gene.secondaryIdentifier Gene.symbol Gene.organism.name Gene.primaryIdentifier\" longDescription=\"\" sortOrder=\"Gene.symbol desc\"></query>"
      {:from "Gene",
       :select
       ["Gene.secondaryIdentifier"
        "Gene.symbol"
        "Gene.organism.name"
        "Gene.primaryIdentifier"],
       :orderBy [{:Gene.symbol "DESC"}],
       :constraintLogic nil,
       :joins [],
       :where []}
      "<query name=\"\" model=\"genomic\" view=\"Gene.secondaryIdentifier Gene.symbol Gene.organism.name Gene.primaryIdentifier\" longDescription=\"\" sortOrder=\"Gene.symbol desc\">
  <constraint path=\"Gene.symbol\" code=\"A\" op=\"CONTAINS\" value=\"ab\"/>
  <constraint path=\"Gene\" code=\"B\" op=\"IN\" value=\"PL FlyAtlas_brain_top\"/>
</query>"
      {:from "Gene",
       :select
       ["Gene.secondaryIdentifier"
        "Gene.symbol"
        "Gene.organism.name"
        "Gene.primaryIdentifier"],
       :orderBy [{:Gene.symbol "DESC"}],
       :constraintLogic nil,
       :joins [],
       :where
       [{:path "Gene.symbol", :code "A", :op "CONTAINS", :value "ab"}
        {:path "Gene", :code "B", :op "IN", :value "PL FlyAtlas_brain_top"}]}
      "<query name=\"\" model=\"genomic\" view=\"Gene.secondaryIdentifier Gene.symbol Gene.organism.name Gene.primaryIdentifier\" longDescription=\"\" sortOrder=\"Gene.symbol desc\" constraintLogic=\"A or B\">
  <constraint path=\"Gene.symbol\" code=\"A\" op=\"CONTAINS\" value=\"ab\"/>
  <constraint path=\"Gene\" code=\"B\" op=\"IN\" value=\"PL FlyAtlas_brain_top\"/>
</query>"
      {:from "Gene",
       :select
       ["Gene.secondaryIdentifier"
        "Gene.symbol"
        "Gene.organism.name"
        "Gene.primaryIdentifier"],
       :orderBy [{:Gene.symbol "DESC"}],
       :constraintLogic "A or B",
       :joins [],
       :where
       [{:path "Gene.symbol", :code "A", :op "CONTAINS", :value "ab"}
        {:path "Gene", :code "B", :op "IN", :value "PL FlyAtlas_brain_top"}]}))
  (testing "Can handle ONE OF constraints"
    (is (= (utils/read-xml-query "<query model=\"genomic\" view=\"Gene.primaryIdentifier Gene.secondaryIdentifier Gene.symbol Gene.name Gene.length Gene.organism.shortName\" constraintLogic=\"(A and B)\">)))
   <constraint path=\"Gene.symbol\" op=\"ONE OF\" code=\"B\"><value>CDPK1</value><value>CK1</value><value>ENO</value><value>CDPK4</value><value>ABRA</value><value>ERD2</value><value>CRK2</value></constraint>))
</query>")
           {:from "Gene"
            :select
            ["Gene.primaryIdentifier"
             "Gene.secondaryIdentifier"
             "Gene.symbol"
             "Gene.name"
             "Gene.length"
             "Gene.organism.shortName"],
            :orderBy []
            :constraintLogic "(A and B)",
            :joins [],
            :where
            [{:path "Gene.symbol",
              :values '("CDPK1" "CK1" "ENO" "CDPK4" "ABRA" "ERD2" "CRK2"),
              :op "ONE OF",
              :code "B"}]})))
  (testing "Can handle OUTER JOIN"
    (is (= (utils/read-xml-query "<query model=\"genomic\" view=\"Gene.symbol Gene.pathways.identifier\">))))
  <join path=\"Gene.pathways\" style=\"OUTER\"/>)
</query>")
           {:from "Gene",
            :select ["Gene.symbol" "Gene.pathways.identifier"],
            :orderBy [],
            :constraintLogic nil,
            :joins ["Gene.pathways"],
            :where []})))
  (testing "Can handle OUTER JOIN mixed with constraints"
    (is (= (utils/read-xml-query "<query name=\"\" model=\"genomic\" view=\"Gene.secondaryIdentifier Gene.symbol Gene.organism.name Gene.primaryIdentifier Gene.proteins.primaryIdentifier Gene.proteins.primaryAccession Gene.proteins.organism.name\" longDescription=\"\" sortOrder=\"Gene.symbol desc\" constraintLogic=\"A and B\">))))
  <constraint path=\"Gene.symbol\" code=\"A\" op=\"CONTAINS\" value=\"ab\"/>
  <join path=\"Gene.proteins\" style=\"OUTER\"/>
  <constraint path=\"Gene\" code=\"B\" op=\"IN\" value=\"PL FlyAtlas_brain_top\"/>)
</query>")
           {:from "Gene",
            :select
            ["Gene.secondaryIdentifier"
             "Gene.symbol"
             "Gene.organism.name"
             "Gene.primaryIdentifier"
             "Gene.proteins.primaryIdentifier"
             "Gene.proteins.primaryAccession"
             "Gene.proteins.organism.name"],
            :orderBy [{:Gene.symbol "DESC"}],
            :constraintLogic "A and B",
            :joins ["Gene.proteins"],
            :where
            [{:path "Gene.symbol", :code "A", :op "CONTAINS", :value "ab"}
             {:path "Gene", :code "B", :op "IN", :value "PL FlyAtlas_brain_top"}]})))
  (testing "Can handle longer paths"
    (is (= (utils/read-xml-query "<query name=\"\" model=\"genomic\" view=\"Gene.homologues.homologue.secondaryIdentifier Gene.homologues.homologue.symbol Gene.homologues.homologue.primaryIdentifier Gene.homologues.homologue.organism.name Gene.secondaryIdentifier Gene.symbol Gene.primaryIdentifier Gene.organism.name\" longDescription=\"\">)))
  <join path=\"Gene.homologues\" style=\"OUTER\"/>
  <constraint path=\"Gene.symbol\" op=\"CONTAINS\" value=\"ab\"/>))
</query>")
           {:from "Gene",
            :select
            ["Gene.homologues.homologue.secondaryIdentifier"
             "Gene.homologues.homologue.symbol"
             "Gene.homologues.homologue.primaryIdentifier"
             "Gene.homologues.homologue.organism.name"
             "Gene.secondaryIdentifier"
             "Gene.symbol"
             "Gene.primaryIdentifier"
             "Gene.organism.name"],
            :orderBy [],
            :constraintLogic nil,
            :joins ["Gene.homologues"],
            :where [{:path "Gene.symbol", :op "CONTAINS", :value "ab"}]}))))

(deftest suitable-entities
  ;; Mock for model classes. We only check that the key is present.
  (let [model {:Gene {} :Protein {}}]
    (is (= (utils/suitable-entities
            model
            {:Gene {:class "Gene" :format "id" :value 1}
             :Protein {:class "Protein" :format "ids" :value [1 2 3]}}
            {:accepts ["id"]
             :classes ["Gene" "Protein"]})
           {:Gene {:class "Gene" :format "id" :value 1}})
        "Should remove invalid formats when accepts id")
    (is (= (utils/suitable-entities
            model
            {:Gene {:class "Gene" :format "id" :value 1}
             :Protein {:class "Protein" :format "ids" :value [1 2 3]}}
            {:accepts ["ids"]
             :classes ["Gene" "Protein"]})
           {:Protein {:class "Protein" :format "ids" :value [1 2 3]}})
        "Should remove invalid formats when accepts ids")
    (is (= (utils/suitable-entities
            model
            {:Gene {:class "Gene" :format "id" :value 1}
             :Protein {:class "Protein" :format "id" :value 2}}
            {:accepts ["id"]
             :classes ["Protein"]})
           {:Protein {:class "Protein" :format "id" :value 2}})
        "Should filter down to config's classes")
    (is (= (utils/suitable-entities
            model
            {:Gene {:class "Gene" :format "id" :value 1}
             :Protein {:class "Protein" :format "ids" :value [1 2 3]}}
            {:accepts ["id" "ids"]
             :classes ["*"]})
           {:Gene {:class "Gene" :format "id" :value 1}
            :Protein {:class "Protein" :format "ids" :value [1 2 3]}})
        "Should handle wildcard in config's classes")
    (is (= (utils/suitable-entities
            model
            {:Gene {:class "Gene" :format "id" :value 1}
             :Protein {:class "Protein" :format "ids" :value [1 2 3]}}
            {:accepts ["id" "ids"]
             :classes ["Gene" "Protein"]
             :depends ["Gene" "Protein"]})
           {:Gene {:class "Gene" :format "id" :value 1}
            :Protein {:class "Protein" :format "ids" :value [1 2 3]}})
        "Should handle depends")
    (testing "Should return nil when no entities are valid"
      (are [entities config suitable]
           (= (utils/suitable-entities model entities config) suitable)

        {:Gene {:class "Gene" :format "id" :value 1}
         :Protein {:class "Protein" :format "id" :value 2}}
        {:accepts ["ids"]
         :classes ["Gene" "Protein"]}
        nil

        {:Gene {:class "Gene" :format "id" :value 1}
         :Protein {:class "Protein" :format "id" :value 2}}
        {:accepts ["id"]
         :classes ["OtherClass"]}
        nil

        {:Gene {:class "Gene" :format "id" :value 1}
         :Protein {:class "Protein" :format "id" :value 2}}
        {:accepts ["id"]
         :classes ["Gene" "Protein"]
         :depends ["NotInModel"]}
        nil

        {:Gene {:class "Gene" :format "id" :value 1}
         :Protein {:class "Protein" :format "id" :value 2}}
        {:accepts ["id"]
         :classes ["*"]
         :depends ["NotInModel"]}
        nil))))
