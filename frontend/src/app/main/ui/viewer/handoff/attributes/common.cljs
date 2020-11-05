;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; This Source Code Form is "Incompatible With Secondary Licenses", as
;; defined by the Mozilla Public License, v. 2.0.
;;
;; Copyright (c) 2020 UXBOX Labs SL

(ns app.main.ui.viewer.handoff.attributes.common
  (:require
   [rumext.alpha :as mf]
   [cuerdas.core :as str]
   [app.util.dom :as dom]
   [app.util.i18n :refer [t] :as i18n]
   [app.util.color :as uc]
   [app.common.math :as mth]
   [app.main.ui.icons :as i]
   [app.util.webapi :as wapi]
   [app.main.ui.components.color-bullet :refer [color-bullet color-name]]))

(defn copy-cb [values properties & {:keys [to-prop format] :or {to-prop {}}}]
  (fn [event]
    (let [
          ;; We allow the :format and :to-prop to be a map for different properties
          ;; or just a value for a single property. This code transform a single
          ;; property to a uniform one
          properties (if-not (coll? properties) [properties] properties)

          format (if (not (map? format))
                   (into {} (map #(vector % format) properties))
                   format)

          to-prop (if (not (map? to-prop))
                    (into {} (map #(vector % to-prop) properties))
                    to-prop)

          default-format (fn [value] (str (mth/precision value 2) "px"))
          format-property (fn [prop]
                            (let [css-prop (or (prop to-prop) (name prop))]
                              (str/fmt "  %s: %s;" css-prop ((or (prop format) default-format) (prop values) values))))

          text-props (->> properties
                          (remove #(let [value (get values %)]
                                     (or (nil? value) (= value 0))))
                          (map format-property)
                          (str/join "\n"))

          result (str/fmt "{\n%s\n}" text-props)]

      (wapi/write-to-clipboard result))))

(mf/defc color-row [{:keys [color format on-copy on-change-format]}]
  (let [locale (mf/deref i18n/locale)]
    [:div.attributes-color-row
     [:& color-bullet {:color color}]

     (if (:gradient color)
       [:& color-name {:color color}]
       (case format
         :rgba (let [[r g b a] (->> (uc/hex->rgba (:color color) (:opacity color)) (map #(mth/precision % 2)))]
                 [:div (str/fmt "%s, %s, %s, %s" r g b a)])
         :hsla (let [[h s l a] (->> (uc/hex->hsla (:color color) (:opacity color)) (map #(mth/precision % 2)))]
                 [:div (str/fmt "%s, %s, %s, %s" h s l a)])
         [:*
          [:& color-name {:color color}]
          (when-not (:gradient color) [:div (str (* 100 (:opacity color)) "%")])]))

     (when-not (and on-change-format (:gradient color))
       [:select {:on-change #(-> (dom/get-target-val %) keyword on-change-format)}
        [:option {:value "hex"}
         (t locale "handoff.attributes.color.hex")]

        [:option {:value "rgba"}
         (t locale "handoff.attributes.color.rgba")]

        [:option {:value "hsla"}
         (t locale "handoff.attributes.color.hsla")]])

     (when on-copy
       [:button.attributes-copy-button {:on-click on-copy} i/copy])]))