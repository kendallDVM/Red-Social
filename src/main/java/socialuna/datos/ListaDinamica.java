package socialuna.datos;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Lista dinámica genérica implementada sobre un arreglo interno que crece
 * automáticamente cuando se llena.
 *
 * <p>Funcionamiento:
 * <ul>
 *   <li>Los elementos se guardan de forma contigua en {@code elementos[0 .. tamano-1]}.</li>
 *   <li>Cuando {@code tamano == elementos.length}, se crea un arreglo del doble de
 *       capacidad y se copian los elementos (redimensionamiento).</li>
 *   <li>Al eliminar, los elementos posteriores se desplazan una posición a la izquierda
 *       para no dejar huecos.</li>
 * </ul>
 *
 * <p>Complejidad:
 * <table>
 *   <caption>Costo de las operaciones</caption>
 *   <tr><td>agregar</td><td>O(1) amortizado (O(n) cuando redimensiona)</td></tr>
 *   <tr><td>obtener / reemplazar</td><td>O(1)</td></tr>
 *   <tr><td>eliminar / indiceDe / contiene</td><td>O(n)</td></tr>
 *   <tr><td>tamano / estaVacia</td><td>O(1)</td></tr>
 * </table>
 *
 * <p>No es segura para uso concurrente (varios hilos). En una aplicación Swing todo
 * se ejecuta en el hilo de eventos, así que no es necesario.
 *
 * @param <T> tipo de los elementos almacenados
 */
public class ListaDinamica<T> implements Iterable<T> {

    /** Capacidad con la que nace la lista si no se indica otra. */
    public static final int CAPACIDAD_INICIAL = 10;

    /** Factor de crecimiento del arreglo interno. */
    private static final int FACTOR_CRECIMIENTO = 2;

    private Object[] elementos;
    private int tamano;

    /**
     * Contador de modificaciones estructurales. Lo usa el iterador para detectar
     * si la lista se modificó mientras se estaba recorriendo.
     */
    private int modificaciones;

    public ListaDinamica() {
        this(CAPACIDAD_INICIAL);
    }

    public ListaDinamica(int capacidadInicial) {
        if (capacidadInicial <= 0) {
            throw new IllegalArgumentException("La capacidad inicial debe ser mayor que cero.");
        }
        this.elementos = new Object[capacidadInicial];
        this.tamano = 0;
    }

    /** Agrega un elemento al final de la lista. */
    public void agregar(T elemento) {
        asegurarCapacidad(tamano + 1);
        elementos[tamano] = elemento;
        tamano++;
        modificaciones++;
    }

    /** Inserta un elemento en la posición indicada, desplazando los siguientes a la derecha. */
    public void insertar(int indice, T elemento) {
        if (indice < 0 || indice > tamano) {
            throw new IndexOutOfBoundsException("Índice " + indice + " fuera de rango [0, " + tamano + "]");
        }
        asegurarCapacidad(tamano + 1);
        for (int i = tamano; i > indice; i--) {
            elementos[i] = elementos[i - 1];
        }
        elementos[indice] = elemento;
        tamano++;
        modificaciones++;
    }

    /** Devuelve el elemento en la posición indicada. */
    @SuppressWarnings("unchecked")
    public T obtener(int indice) {
        validarIndice(indice);
        return (T) elementos[indice];
    }

    /** Reemplaza el elemento de una posición y devuelve el anterior. */
    public T reemplazar(int indice, T elemento) {
        T anterior = obtener(indice);
        elementos[indice] = elemento;
        return anterior;
    }

    /** Elimina el elemento de la posición indicada y lo devuelve. */
    public T eliminar(int indice) {
        T eliminado = obtener(indice);
        for (int i = indice; i < tamano - 1; i++) {
            elementos[i] = elementos[i + 1];
        }
        tamano--;
        elementos[tamano] = null; // liberar la referencia para el recolector de basura
        modificaciones++;
        return eliminado;
    }

    /**
     * Elimina la primera aparición del elemento (comparando con {@code equals}).
     *
     * @return {@code true} si se encontró y eliminó
     */
    public boolean eliminarElemento(T elemento) {
        int indice = indiceDe(elemento);
        if (indice < 0) {
            return false;
        }
        eliminar(indice);
        return true;
    }

    /** Devuelve la posición de la primera aparición del elemento, o -1 si no está. */
    public int indiceDe(T elemento) {
        for (int i = 0; i < tamano; i++) {
            Object actual = elementos[i];
            if (elemento == null ? actual == null : elemento.equals(actual)) {
                return i;
            }
        }
        return -1;
    }

    public boolean contiene(T elemento) {
        return indiceDe(elemento) >= 0;
    }

    public int tamano() {
        return tamano;
    }

    public boolean estaVacia() {
        return tamano == 0;
    }

    /** Capacidad actual del arreglo interno (útil para demostrar el crecimiento). */
    public int capacidad() {
        return elementos.length;
    }

    /** Elimina todos los elementos. Conserva la capacidad actual. */
    public void limpiar() {
        for (int i = 0; i < tamano; i++) {
            elementos[i] = null;
        }
        tamano = 0;
        modificaciones++;
    }

    /** Copia los elementos a un arreglo del tamaño exacto (por ejemplo, para serializar a JSON). */
    public T[] aArreglo(T[] destino) {
        T[] resultado = destino.length >= tamano
                ? destino
                : java.util.Arrays.copyOf(destino, tamano);
        System.arraycopy(elementos, 0, resultado, 0, tamano);
        if (resultado.length > tamano) {
            resultado[tamano] = null;
        }
        return resultado;
    }

    /** Crea una copia superficial (los objetos contenidos son los mismos). */
    public ListaDinamica<T> copia() {
        ListaDinamica<T> copia = new ListaDinamica<>(Math.max(tamano, CAPACIDAD_INICIAL));
        for (int i = 0; i < tamano; i++) {
            copia.agregar(obtener(i));
        }
        return copia;
    }

    private void asegurarCapacidad(int capacidadMinima) {
        if (capacidadMinima > elementos.length) {
            int nuevaCapacidad = Math.max(elementos.length * FACTOR_CRECIMIENTO, capacidadMinima);
            Object[] nuevo = new Object[nuevaCapacidad];
            for (int i = 0; i < tamano; i++) {
                nuevo[i] = elementos[i];
            }
            elementos = nuevo;
        }
    }

    private void validarIndice(int indice) {
        if (indice < 0 || indice >= tamano) {
            throw new IndexOutOfBoundsException("Índice " + indice + " fuera de rango [0, " + (tamano - 1) + "]");
        }
    }

    /** Permite recorrer la lista con {@code for (T e : lista)}. */
    @Override
    public Iterator<T> iterator() {
        return new Iterator<>() {
            private int posicion = 0;
            private final int modificacionesEsperadas = modificaciones;

            @Override
            public boolean hasNext() {
                return posicion < tamano;
            }

            @Override
            public T next() {
                if (modificaciones != modificacionesEsperadas) {
                    throw new ConcurrentModificationException(
                            "La lista se modificó mientras se recorría.");
                }
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return obtener(posicion++);
            }
        };
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < tamano; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(elementos[i]);
        }
        return sb.append(']').toString();
    }
}
